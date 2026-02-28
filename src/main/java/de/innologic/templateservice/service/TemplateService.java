package de.innologic.templateservice.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import de.innologic.templateservice.api.dto.RenderRequest;
import de.innologic.templateservice.api.dto.RenderResponse;
import de.innologic.templateservice.api.dto.PageResponse;
import de.innologic.templateservice.api.dto.CatalogTemplateFamilyResponse;
import de.innologic.templateservice.api.dto.CatalogTemplateVersionResponse;
import de.innologic.templateservice.api.dto.TemplateFamilyRequest;
import de.innologic.templateservice.api.dto.TemplateFamilyResponse;
import de.innologic.templateservice.api.dto.TemplateVersionRequest;
import de.innologic.templateservice.api.dto.TemplateVersionResponse;
import de.innologic.templateservice.api.dto.ValidateTemplateRequest;
import de.innologic.templateservice.api.dto.ValidateTemplateResponse;
import de.innologic.templateservice.api.error.ConflictException;
import de.innologic.templateservice.api.error.NotFoundException;
import de.innologic.templateservice.api.error.UnprocessableTemplateException;
import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.entity.TemplateVersion;
import de.innologic.templateservice.domain.enums.MissingKeyPolicy;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import de.innologic.templateservice.domain.repository.TemplateFamilyRepository;
import de.innologic.templateservice.domain.repository.TemplateVersionRepository;
import de.innologic.templateservice.events.TemplateEventPublisher;
import de.innologic.templateservice.events.TemplateFamilyCreatedEvent;
import de.innologic.templateservice.events.TemplateFamilyUpdatedEvent;
import de.innologic.templateservice.events.TemplateVersionCreatedEvent;
import de.innologic.templateservice.events.TemplateVersionApprovedEvent;
import de.innologic.templateservice.events.TemplateVersionDeprecatedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import de.innologic.templateservice.security.TenantContext;
import de.innologic.templateservice.security.TenantContextResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Instant;

@Service
@Transactional
public class TemplateService {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");
    private static final String GLOBAL_OWNER = "__GLOBAL__";

    private final TemplateFamilyRepository templateFamilyRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final JsonMapper jsonMapper;
    private final TenantContextResolver tenantContextResolver;
    private final CachedTemplateLookupService cachedTemplateLookupService;
    private final TemplateEventPublisher templateEventPublisher;
    private final String defaultLocale;

    public TemplateService(
            TemplateFamilyRepository templateFamilyRepository,
            TemplateVersionRepository templateVersionRepository,
            JsonMapper jsonMapper,
            TenantContextResolver tenantContextResolver,
            CachedTemplateLookupService cachedTemplateLookupService,
            TemplateEventPublisher templateEventPublisher,
            @Value("${template.resolve.default-locale:en-GB}") String defaultLocale
    ) {
        this.templateFamilyRepository = templateFamilyRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.jsonMapper = jsonMapper;
        this.tenantContextResolver = tenantContextResolver;
        this.cachedTemplateLookupService = cachedTemplateLookupService;
        this.templateEventPublisher = templateEventPublisher;
        this.defaultLocale = defaultLocale;
    }

    public TemplateFamilyResponse createFamily(TemplateFamilyRequest request) {
        TenantContext tenantContext = tenantContextResolver.resolveRequired();
        String actor = tenantContext.actor();
        enforceFamilyGovernance(request, null);
        TemplateFamily family = new TemplateFamily();
        family.setTemplateId(UUID.randomUUID());
        applyFamilyRequest(family, request);
        family.setCreatedBy(actor);
        family.setUpdatedBy(actor);
        TemplateFamily saved = templateFamilyRepository.save(family);
        publishFamilyCreated(saved, actor);
        evictLookupCaches();
        return toFamilyResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<TemplateFamilyResponse> listFamilies(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        String tenantId = tenantContextResolver.resolveRequired().tenantId();
        Page<TemplateFamily> families = templateFamilyRepository.findVisibleFamilies(
                TemplateScope.GLOBAL,
                GLOBAL_OWNER,
                TemplateScope.TENANT,
                tenantId,
                pageable
        );
        List<TemplateFamilyResponse> items = families.getContent().stream().map(this::toFamilyResponse).toList();
        return new PageResponse<>(items, families.getNumber(), families.getSize(), families.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogTemplateFamilyResponse> catalogFamilies(
            TemplateScope scope,
            String channel,
            String locale,
            int page,
            int size,
            String sort
    ) {
        String ownerTenantId = scope == TemplateScope.GLOBAL
                ? GLOBAL_OWNER
                : tenantContextResolver.resolveRequired().tenantId();
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<TemplateFamily> families = templateFamilyRepository.findByScopeAndOwnerTenantIdAndChannelAndLocale(
                scope, ownerTenantId, channel, locale, pageable
        );
        List<CatalogTemplateFamilyResponse> items = families.getContent().stream()
                .map(this::toCatalogFamily)
                .toList();
        return new PageResponse<>(items, families.getNumber(), families.getSize(), families.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogTemplateVersionResponse> catalogApprovedVersions(
            UUID templateId,
            int page,
            int size,
            String sort
    ) {
        requireFamily(templateId);
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<TemplateVersion> versions = templateVersionRepository.findByTemplateIdAndStatus(
                templateId,
                TemplateStatus.APPROVED,
                pageable
        );
        List<CatalogTemplateVersionResponse> items = versions.getContent().stream()
                .map(this::toCatalogVersion)
                .toList();
        return new PageResponse<>(items, versions.getNumber(), versions.getSize(), versions.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TemplateFamilyResponse getFamily(UUID templateId) {
        return toFamilyResponse(requireFamily(templateId));
    }

    public TemplateFamilyResponse updateFamily(UUID templateId, TemplateFamilyRequest request) {
        TenantContext tenantContext = tenantContextResolver.resolveRequired();
        String actor = tenantContext.actor();
        enforceFamilyGovernance(request, templateId);
        TemplateFamily family = requireFamily(templateId);
        applyFamilyRequest(family, request);
        family.setUpdatedBy(actor);
        TemplateFamily saved = templateFamilyRepository.save(family);
        publishFamilyUpdated(saved, actor);
        evictLookupCaches();
        return toFamilyResponse(saved);
    }

    public void deleteFamily(UUID templateId) {
        requireFamily(templateId);
        templateVersionRepository.deleteByTemplateId(templateId);
        templateFamilyRepository.deleteById(templateId);
        evictLookupCaches();
    }

    public TemplateVersionResponse createVersion(UUID templateId, TemplateVersionRequest request) {
        TemplateFamily family = requireFamily(templateId);

        Integer nextVersionNo = templateVersionRepository.findFirstByTemplateIdOrderByVersionNoDesc(templateId)
                .map(version -> version.getVersionNo() + 1)
                .orElse(1);

        String actor = tenantContextResolver.resolveRequired().actor();

        TemplateVersion version = new TemplateVersion();
        version.setVersionId(UUID.randomUUID());
        version.setTemplateId(family.getTemplateId());
        version.setVersionNo(nextVersionNo);
        applyVersionRequest(version, request);
        version.setStatus(TemplateStatus.DRAFT);
        version.setCreatedBy(actor);
        version.setUpdatedBy(actor);

        TemplateVersion saved = templateVersionRepository.save(version);
        publishVersionCreated(family, saved, actor);
        evictLookupCaches();
        return toVersionResponse(saved);
    }

    @Transactional(readOnly = true)
    public boolean isGlobalFamily(UUID templateId) {
        return requireFamily(templateId).getScope() == TemplateScope.GLOBAL;
    }

    @Transactional(readOnly = true)
    public PageResponse<TemplateVersionResponse> listVersions(UUID templateId, int page, int size, String sort) {
        requireFamily(templateId);
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<TemplateVersion> versions = templateVersionRepository.findByTemplateId(templateId, pageable);
        List<TemplateVersionResponse> items = versions.getContent().stream().map(this::toVersionResponse).toList();
        return new PageResponse<>(items, versions.getNumber(), versions.getSize(), versions.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TemplateVersionResponse getVersion(UUID templateId, Integer versionNo) {
        return toVersionResponse(requireVersionVisible(templateId, versionNo));
    }

    public TemplateVersionResponse updateVersion(UUID templateId, Integer versionNo, TemplateVersionRequest request) {
        throw new ConflictException("VERSION_IMMUTABLE");
    }

    public void deleteVersion(UUID templateId, Integer versionNo) {
        requireFamily(templateId);
        TemplateVersion version = requireVersion(templateId, versionNo);
        templateVersionRepository.delete(version);
        evictLookupCaches();
    }

    public TemplateVersionResponse approveVersion(UUID templateId, Integer versionNo) {
        TemplateFamily family = requireFamily(templateId);
        TemplateVersion selected = requireVersion(templateId, versionNo);
        if (selected.getStatus() == TemplateStatus.ARCHIVED) {
            throw new ConflictException("VERSION_NOT_RENDERABLE");
        }

        String actor = tenantContextResolver.resolveRequired().actor();

        List<TemplateVersion> versions = templateVersionRepository.findByTemplateIdOrderByVersionNoDesc(templateId);
        List<Integer> deprecatedVersionNos = new ArrayList<>();
        for (TemplateVersion version : versions) {
            if (version.getVersionNo().equals(versionNo)) {
                version.setStatus(TemplateStatus.APPROVED);
                version.setUpdatedBy(actor);
                selected = version;
            } else if (version.getStatus() == TemplateStatus.APPROVED) {
                version.setStatus(TemplateStatus.DEPRECATED);
                version.setUpdatedBy(actor);
                deprecatedVersionNos.add(version.getVersionNo());
            }
        }
        templateVersionRepository.saveAll(versions);

        family.setActiveApprovedVersion(versionNo);
        family.setUpdatedBy(actor);
        templateFamilyRepository.save(family);
        publishVersionApproved(family, selected, actor);
        for (Integer deprecatedVersionNo : deprecatedVersionNos) {
            publishVersionDeprecated(family, deprecatedVersionNo, actor);
        }
        evictLookupCaches();

        return toVersionResponse(selected);
    }

    @Transactional(readOnly = true)
    public RenderResponse renderApproved(RenderRequest request) {
        ResolvedTemplate resolved = resolveTemplateFamily(request);
        TemplateFamily family = resolved.family();

        TemplateVersion version = cachedTemplateLookupService.resolveApprovedVersion(
                        family.getTemplateId(),
                        family.getActiveApprovedVersion()
                )
                .orElseThrow(() -> new UnprocessableTemplateException(
                        "No APPROVED version available for template " + family.getTemplateId()
                ));

        return renderResponse(resolved, version, request.effectiveVariables(), request.effectiveMissingKeyPolicy());
    }

    @Transactional(readOnly = true)
    public RenderResponse preview(RenderRequest request) {
        ResolvedTemplate resolved = resolveTemplateFamily(request);
        TemplateFamily family = resolved.family();

        TemplateVersion version = Optional.ofNullable(request.versionNo())
                .flatMap(versionNo -> templateVersionRepository.findByTemplateIdAndVersionNo(family.getTemplateId(), versionNo))
                .or(() -> templateVersionRepository.findFirstByTemplateIdOrderByVersionNoDesc(family.getTemplateId()))
                .orElseThrow(() -> new NotFoundException(
                        "VERSION_NOT_FOUND",
                        "Template version not found for templateId=%s versionNo=%s".formatted(
                                family.getTemplateId(),
                                request.versionNo()
                        )
                ));
        if (version.getStatus() == TemplateStatus.ARCHIVED) {
            throw new UnprocessableTemplateException("VERSION_NOT_RENDERABLE");
        }

        return renderResponse(resolved, version, request.effectiveVariables(), request.effectiveMissingKeyPolicy());
    }

    @Transactional(readOnly = true)
    public ValidateTemplateResponse validate(ValidateTemplateRequest request) {
        List<String> detected = detectPlaceholders(request.subjectTpl(), request.bodyTpl());
        List<String> declared = parseDeclaredPlaceholders(request.placeholders());

        List<String> errors = new ArrayList<>();
        for (String placeholder : detected) {
            if (!declared.contains(placeholder)) {
                errors.add("Detected placeholder not declared: " + placeholder);
            }
        }
        for (String placeholder : declared) {
            if (!detected.contains(placeholder)) {
                errors.add("Declared placeholder not used: " + placeholder);
            }
        }

        return new ValidateTemplateResponse(errors.isEmpty(), detected, declared, errors);
    }

    private RenderResponse renderResponse(
            ResolvedTemplate resolved,
            TemplateVersion version,
            Map<String, Object> model,
            MissingKeyPolicy missingKeyPolicy
    ) {
        Map<String, Object> safeModel = model == null ? Collections.emptyMap() : model;

        RenderedText renderedSubject = renderTemplate(version.getSubjectTpl(), safeModel, missingKeyPolicy, version.getRenderTarget());
        RenderedText renderedBody = renderTemplate(version.getBodyTpl(), safeModel, missingKeyPolicy, version.getRenderTarget());
        List<String> warnings = mergeWarnings(renderedSubject.warnings(), renderedBody.warnings());

        return new RenderResponse(
                resolved.family().getScope(),
                version.getTemplateId(),
                resolved.resolvedLocale(),
                version.getVersionNo(),
                version.getStatus(),
                version.getRenderTarget(),
                version.getRenderTarget() == de.innologic.templateservice.domain.enums.RenderTarget.HTML ? "text/html" : "text/plain",
                renderedSubject.text(),
                renderedBody.text(),
                warnings
        );
    }

    private RenderedText renderTemplate(
            String template,
            Map<String, Object> model,
            MissingKeyPolicy missingKeyPolicy,
            de.innologic.templateservice.domain.enums.RenderTarget renderTarget
    ) {
        if (template == null) {
            return new RenderedText(null, List.of());
        }

        String rendered = template;
        LinkedHashSet<String> missingKeys = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = model.get(key);
            if (value == null) {
                missingKeys.add(key);
                if (missingKeyPolicy == MissingKeyPolicy.EMPTY) {
                    rendered = rendered.replace(matcher.group(0), "");
                }
                continue;
            }
            String replacement = String.valueOf(value);
            if (renderTarget == de.innologic.templateservice.domain.enums.RenderTarget.HTML) {
                replacement = escapeHtml(replacement);
            } else {
                replacement = normalizeText(replacement);
            }
            rendered = rendered.replace(matcher.group(0), replacement);
        }
        if (!missingKeys.isEmpty() && missingKeyPolicy == MissingKeyPolicy.FAIL) {
            List<String> keys = List.copyOf(missingKeys);
            throw new UnprocessableTemplateException("MISSING_KEYS", keys);
        }
        if (renderTarget == de.innologic.templateservice.domain.enums.RenderTarget.TEXT) {
            rendered = normalizeText(rendered);
        }
        List<String> warnings = (!missingKeys.isEmpty() && missingKeyPolicy != MissingKeyPolicy.FAIL)
                ? List.of("MISSING_KEYS missingKeys=" + missingKeys)
                : List.of();
        return new RenderedText(rendered, warnings);
    }

    private ResolvedTemplate resolveTemplateFamily(RenderRequest request) {
        if (request.templateId() != null) {
            TemplateFamily family = requireFamily(request.templateId());
            return new ResolvedTemplate(family, family.getLocale());
        }

        String tenantId = tenantContextResolver.resolveRequired().tenantId();
        TemplateScope scope = request.scope();
        String ownerTenantId = scope == TemplateScope.GLOBAL ? "__GLOBAL__" : tenantId;

        for (String localeCandidate : localeCandidates(request.locale())) {
            Optional<CachedTemplateLookupService.ResolvedFamilyLookup> match = cachedTemplateLookupService.resolveTemplateFamily(
                    scope,
                    ownerTenantId,
                    request.templateKey(),
                    request.channel(),
                    localeCandidate
            );
            if (match.isPresent()) {
                return new ResolvedTemplate(match.get().family(), match.get().resolvedLocale());
            }
        }

        throw new NotFoundException(
                "TEMPLATE_NOT_FOUND",
                "Template family not found for key=%s channel=%s locale=%s".formatted(
                        request.templateKey(), request.channel(), request.locale()
                )
        );
    }

    private List<String> localeCandidates(String requestedLocale) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (StringUtils.hasText(requestedLocale)) {
            candidates.add(requestedLocale);
            int separator = requestedLocale.indexOf('-');
            if (separator > 0) {
                candidates.add(requestedLocale.substring(0, separator));
            }
        }
        if (StringUtils.hasText(defaultLocale)) {
            candidates.add(defaultLocale);
        }
        return List.copyOf(candidates);
    }

    private List<String> detectPlaceholders(String subjectTpl, String bodyTpl) {
        LinkedHashSet<String> placeholders = new LinkedHashSet<>();
        collectPlaceholders(placeholders, subjectTpl);
        collectPlaceholders(placeholders, bodyTpl);
        return new ArrayList<>(placeholders);
    }

    private void collectPlaceholders(LinkedHashSet<String> placeholders, String template) {
        if (template == null || template.isBlank()) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
    }

    private List<String> parseDeclaredPlaceholders(String placeholders) {
        if (placeholders == null || placeholders.isBlank()) {
            return List.of();
        }
        try {
            List<String> parsed = jsonMapper.readValue(placeholders, new TypeReference<List<String>>() {});
            return parsed == null
                    ? List.of()
                    : parsed.stream().filter(v -> v != null && !v.isBlank()).toList();
        } catch (Exception ignored) {
            return List.of(placeholders.replace("[", "")
                            .replace("]", "")
                            .replace("\"", "")
                            .split(","))
                    .stream()
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .toList();
        }
    }

    private void applyFamilyRequest(TemplateFamily family, TemplateFamilyRequest request) {
        family.setScope(request.scope());
        family.setOwnerTenantId(request.ownerTenantId());
        family.setTemplateKey(request.templateKey());
        family.setChannel(request.channel());
        family.setLocale(request.locale());
        family.setCategory(request.category());
    }

    private void applyVersionRequest(TemplateVersion version, TemplateVersionRequest request) {
        version.setRenderTarget(request.renderTarget());
        version.setSubjectTpl(request.subjectTpl());
        version.setBodyTpl(request.bodyTpl());
        version.setPlaceholders(extractPlaceholdersAsJson(request.subjectTpl(), request.bodyTpl()));
    }

    private void enforceFamilyGovernance(TemplateFamilyRequest request, UUID existingTemplateId) {
        String key = request.templateKey() == null ? "" : request.templateKey().toLowerCase();
        boolean reservedNamespace = key.startsWith("platform.") || key.startsWith("system.");
        if (reservedNamespace && request.scope() != TemplateScope.GLOBAL) {
            throw new UnprocessableTemplateException("TEMPLATE_KEY_RESERVED");
        }

        if (request.scope() == TemplateScope.TENANT) {
            boolean shadowingGlobalExists = templateFamilyRepository.existsByScopeAndOwnerTenantIdAndTemplateKeyAndChannelAndLocale(
                    TemplateScope.GLOBAL,
                    GLOBAL_OWNER,
                    request.templateKey(),
                    request.channel(),
                    request.locale()
            );
            if (shadowingGlobalExists) {
                if (existingTemplateId == null) {
                    throw new ConflictException("TEMPLATE_SHADOWING_FORBIDDEN");
                }
                TemplateFamily existing = requireFamily(existingTemplateId);
                boolean isSameBusinessKey = request.templateKey().equals(existing.getTemplateKey())
                        && request.channel().equals(existing.getChannel())
                        && request.locale().equals(existing.getLocale());
                if (!isSameBusinessKey) {
                    throw new ConflictException("TEMPLATE_SHADOWING_FORBIDDEN");
                }
            }
        }
    }

    private String extractPlaceholdersAsJson(String subjectTpl, String bodyTpl) {
        try {
            return jsonMapper.writeValueAsString(detectPlaceholders(subjectTpl, bodyTpl));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize placeholders", ex);
        }
    }

    private List<String> mergeWarnings(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(first);
        merged.addAll(second);
        return List.copyOf(merged);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String normalizeText(String value) {
        StringBuilder normalized = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t' || !Character.isISOControl(c)) {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    private void evictLookupCaches() {
        cachedTemplateLookupService.evictAll();
    }

    private void publishFamilyCreated(TemplateFamily family, String actorSub) {
        templateEventPublisher.publish(new TemplateFamilyCreatedEvent(
                family.getOwnerTenantId(),
                family.getScope().name(),
                family.getTemplateId(),
                null,
                actorSub,
                Instant.now()
        ));
    }

    private void publishFamilyUpdated(TemplateFamily family, String actorSub) {
        templateEventPublisher.publish(new TemplateFamilyUpdatedEvent(
                family.getOwnerTenantId(),
                family.getScope().name(),
                family.getTemplateId(),
                null,
                actorSub,
                Instant.now()
        ));
    }

    private void publishVersionCreated(TemplateFamily family, TemplateVersion version, String actorSub) {
        templateEventPublisher.publish(new TemplateVersionCreatedEvent(
                family.getOwnerTenantId(),
                family.getScope().name(),
                family.getTemplateId(),
                version.getVersionNo(),
                actorSub,
                Instant.now()
        ));
    }

    private void publishVersionApproved(TemplateFamily family, TemplateVersion version, String actorSub) {
        templateEventPublisher.publish(new TemplateVersionApprovedEvent(
                family.getOwnerTenantId(),
                family.getScope().name(),
                family.getTemplateId(),
                version.getVersionNo(),
                actorSub,
                Instant.now()
        ));
    }

    private void publishVersionDeprecated(TemplateFamily family, Integer versionNo, String actorSub) {
        templateEventPublisher.publish(new TemplateVersionDeprecatedEvent(
                family.getOwnerTenantId(),
                family.getScope().name(),
                family.getTemplateId(),
                versionNo,
                actorSub,
                Instant.now()
        ));
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",", 2);
        String property = parts[0].trim();
        if (!StringUtils.hasText(property)) {
            property = "createdAt";
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1 && StringUtils.hasText(parts[1])) {
            direction = Sort.Direction.fromOptionalString(parts[1].trim()).orElse(Sort.Direction.ASC);
        }
        return Sort.by(direction, property);
    }

    private TemplateFamily requireFamily(UUID templateId) {
        String tenantId = tenantContextResolver.resolveRequired().tenantId();
        return templateFamilyRepository.findVisibleByTemplateId(
                templateId,
                TemplateScope.TENANT,
                tenantId,
                TemplateScope.GLOBAL,
                GLOBAL_OWNER
        ).orElseThrow(() -> new NotFoundException("TEMPLATE_NOT_FOUND", "Template family not found: " + templateId));
    }

    private TemplateVersion requireVersion(UUID templateId, Integer versionNo) {
        return templateVersionRepository.findByTemplateIdAndVersionNo(templateId, versionNo)
                .orElseThrow(() -> new NotFoundException(
                        "VERSION_NOT_FOUND",
                        "Template version not found for templateId=%s versionNo=%d".formatted(templateId, versionNo)
                ));
    }

    private TemplateVersion requireVersionVisible(UUID templateId, Integer versionNo) {
        requireFamily(templateId);
        return requireVersion(templateId, versionNo);
    }

    private TemplateFamilyResponse toFamilyResponse(TemplateFamily family) {
        return new TemplateFamilyResponse(
                family.getTemplateId(),
                family.getScope(),
                family.getOwnerTenantId(),
                family.getTemplateKey(),
                family.getChannel(),
                family.getLocale(),
                family.getCategory(),
                family.getActiveApprovedVersion(),
                family.getCreatedAt(),
                family.getCreatedBy(),
                family.getUpdatedAt(),
                family.getUpdatedBy()
        );
    }

    private TemplateVersionResponse toVersionResponse(TemplateVersion version) {
        return new TemplateVersionResponse(
                version.getVersionId(),
                version.getTemplateId(),
                version.getVersionNo(),
                version.getStatus(),
                version.getRenderTarget(),
                version.getSubjectTpl(),
                version.getBodyTpl(),
                version.getPlaceholders(),
                version.getCreatedAt(),
                version.getCreatedBy(),
                version.getUpdatedAt(),
                version.getUpdatedBy()
        );
    }

    private CatalogTemplateFamilyResponse toCatalogFamily(TemplateFamily family) {
        return new CatalogTemplateFamilyResponse(
                family.getTemplateId(),
                family.getTemplateKey(),
                family.getChannel(),
                family.getLocale(),
                family.getCategory(),
                family.getActiveApprovedVersion()
        );
    }

    private CatalogTemplateVersionResponse toCatalogVersion(TemplateVersion version) {
        return new CatalogTemplateVersionResponse(
                version.getVersionId(),
                version.getTemplateId(),
                version.getVersionNo(),
                version.getStatus(),
                version.getRenderTarget(),
                version.getCreatedAt()
        );
    }

    private record ResolvedTemplate(
            TemplateFamily family,
            String resolvedLocale
    ) {
    }

    private record RenderedText(
            String text,
            List<String> warnings
    ) {
    }
}
