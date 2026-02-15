package de.innologic.templateservice.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import de.innologic.templateservice.api.dto.RenderRequest;
import de.innologic.templateservice.api.dto.RenderResponse;
import de.innologic.templateservice.api.dto.TemplateFamilyRequest;
import de.innologic.templateservice.api.dto.TemplateFamilyResponse;
import de.innologic.templateservice.api.dto.TemplateVersionRequest;
import de.innologic.templateservice.api.dto.TemplateVersionResponse;
import de.innologic.templateservice.api.dto.ValidateTemplateRequest;
import de.innologic.templateservice.api.dto.ValidateTemplateResponse;
import de.innologic.templateservice.api.error.NotFoundException;
import de.innologic.templateservice.api.error.UnprocessableTemplateException;
import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.entity.TemplateVersion;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import de.innologic.templateservice.domain.repository.TemplateFamilyRepository;
import de.innologic.templateservice.domain.repository.TemplateVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class TemplateService {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");

    private final TemplateFamilyRepository templateFamilyRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final JsonMapper jsonMapper;

    public TemplateService(
            TemplateFamilyRepository templateFamilyRepository,
            TemplateVersionRepository templateVersionRepository,
            JsonMapper jsonMapper
    ) {
        this.templateFamilyRepository = templateFamilyRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.jsonMapper = jsonMapper;
    }

    public TemplateFamilyResponse createFamily(TemplateFamilyRequest request) {
        TemplateFamily family = new TemplateFamily();
        family.setTemplateId(UUID.randomUUID());
        applyFamilyRequest(family, request);
        family.setCreatedBy(request.updatedBy());
        family.setUpdatedBy(request.updatedBy());
        return toFamilyResponse(templateFamilyRepository.save(family));
    }

    @Transactional(readOnly = true)
    public List<TemplateFamilyResponse> listFamilies() {
        return templateFamilyRepository.findAll().stream().map(this::toFamilyResponse).toList();
    }

    @Transactional(readOnly = true)
    public TemplateFamilyResponse getFamily(UUID templateId) {
        return toFamilyResponse(requireFamily(templateId));
    }

    public TemplateFamilyResponse updateFamily(UUID templateId, TemplateFamilyRequest request) {
        TemplateFamily family = requireFamily(templateId);
        applyFamilyRequest(family, request);
        family.setUpdatedBy(request.updatedBy());
        return toFamilyResponse(templateFamilyRepository.save(family));
    }

    public void deleteFamily(UUID templateId) {
        requireFamily(templateId);
        templateVersionRepository.deleteByTemplateId(templateId);
        templateFamilyRepository.deleteById(templateId);
    }

    public TemplateVersionResponse createVersion(UUID templateId, TemplateVersionRequest request) {
        TemplateFamily family = requireFamily(templateId);

        Integer nextVersionNo = Optional.ofNullable(request.versionNo())
                .orElseGet(() -> templateVersionRepository.findFirstByTemplateIdOrderByVersionNoDesc(templateId)
                        .map(version -> version.getVersionNo() + 1)
                        .orElse(1));

        TemplateVersion version = new TemplateVersion();
        version.setVersionId(UUID.randomUUID());
        version.setTemplateId(family.getTemplateId());
        version.setVersionNo(nextVersionNo);
        applyVersionRequest(version, request);
        version.setCreatedBy(request.updatedBy());
        version.setUpdatedBy(request.updatedBy());

        return toVersionResponse(templateVersionRepository.save(version));
    }

    @Transactional(readOnly = true)
    public List<TemplateVersionResponse> listVersions(UUID templateId) {
        requireFamily(templateId);
        return templateVersionRepository.findByTemplateIdOrderByVersionNoDesc(templateId).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateVersionResponse getVersion(UUID templateId, Integer versionNo) {
        return toVersionResponse(requireVersion(templateId, versionNo));
    }

    public TemplateVersionResponse updateVersion(UUID templateId, Integer versionNo, TemplateVersionRequest request) {
        requireFamily(templateId);
        TemplateVersion version = requireVersion(templateId, versionNo);
        applyVersionRequest(version, request);
        version.setUpdatedBy(request.updatedBy());
        return toVersionResponse(templateVersionRepository.save(version));
    }

    public void deleteVersion(UUID templateId, Integer versionNo) {
        requireFamily(templateId);
        TemplateVersion version = requireVersion(templateId, versionNo);
        templateVersionRepository.delete(version);
    }

    public TemplateVersionResponse approveVersion(UUID templateId, Integer versionNo, String updatedBy) {
        TemplateFamily family = requireFamily(templateId);
        TemplateVersion selected = requireVersion(templateId, versionNo);

        List<TemplateVersion> versions = templateVersionRepository.findByTemplateIdOrderByVersionNoDesc(templateId);
        for (TemplateVersion version : versions) {
            if (version.getVersionNo().equals(versionNo)) {
                version.setStatus(TemplateStatus.APPROVED);
                version.setUpdatedBy(updatedBy);
                selected = version;
            } else if (version.getStatus() == TemplateStatus.APPROVED) {
                version.setStatus(TemplateStatus.DEPRECATED);
                version.setUpdatedBy(updatedBy);
            }
        }
        templateVersionRepository.saveAll(versions);

        family.setActiveApprovedVersion(versionNo);
        family.setUpdatedBy(updatedBy);
        templateFamilyRepository.save(family);

        return toVersionResponse(selected);
    }

    @Transactional(readOnly = true)
    public RenderResponse renderApproved(RenderRequest request) {
        TemplateFamily family = requireFamily(request.templateId());

        TemplateVersion version = Optional.ofNullable(family.getActiveApprovedVersion())
                .flatMap(versionNo -> templateVersionRepository.findByTemplateIdAndVersionNo(family.getTemplateId(), versionNo))
                .filter(found -> found.getStatus() == TemplateStatus.APPROVED)
                .or(() -> templateVersionRepository.findFirstByTemplateIdAndStatusOrderByVersionNoDesc(family.getTemplateId(), TemplateStatus.APPROVED))
                .orElseThrow(() -> new UnprocessableTemplateException(
                        "No APPROVED version available for template " + family.getTemplateId()
                ));

        return renderResponse(version, request.model());
    }

    @Transactional(readOnly = true)
    public RenderResponse preview(RenderRequest request) {
        requireFamily(request.templateId());

        TemplateVersion version = Optional.ofNullable(request.versionNo())
                .flatMap(versionNo -> templateVersionRepository.findByTemplateIdAndVersionNo(request.templateId(), versionNo))
                .or(() -> templateVersionRepository.findFirstByTemplateIdOrderByVersionNoDesc(request.templateId()))
                .orElseThrow(() -> new NotFoundException("No version found for template " + request.templateId()));

        return renderResponse(version, request.model());
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

    private RenderResponse renderResponse(TemplateVersion version, Map<String, Object> model) {
        Map<String, Object> safeModel = model == null ? Collections.emptyMap() : model;

        String renderedSubject = renderTemplate(version.getSubjectTpl(), safeModel);
        String renderedBody = renderTemplate(version.getBodyTpl(), safeModel);

        return new RenderResponse(
                version.getTemplateId(),
                version.getVersionNo(),
                version.getStatus(),
                version.getRenderTarget(),
                renderedSubject,
                renderedBody
        );
    }

    private String renderTemplate(String template, Map<String, Object> model) {
        if (template == null) {
            return null;
        }

        String rendered = template;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!model.containsKey(key) || model.get(key) == null) {
                throw new UnprocessableTemplateException("Missing model value for placeholder: " + key);
            }
            rendered = rendered.replace(matcher.group(0), String.valueOf(model.get(key)));
        }
        return rendered;
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
        version.setStatus(request.status());
        version.setRenderTarget(request.renderTarget());
        version.setSubjectTpl(request.subjectTpl());
        version.setBodyTpl(request.bodyTpl());
        version.setPlaceholders(request.placeholders());
    }

    private TemplateFamily requireFamily(UUID templateId) {
        return templateFamilyRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template family not found: " + templateId));
    }

    private TemplateVersion requireVersion(UUID templateId, Integer versionNo) {
        return templateVersionRepository.findByTemplateIdAndVersionNo(templateId, versionNo)
                .orElseThrow(() -> new NotFoundException(
                        "Template version not found for templateId=%s versionNo=%d".formatted(templateId, versionNo)
                ));
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
}

