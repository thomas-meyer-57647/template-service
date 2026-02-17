package de.innologic.templateservice.service;

import de.innologic.templateservice.config.CacheConfig;
import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.entity.TemplateVersion;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import de.innologic.templateservice.domain.repository.TemplateFamilyRepository;
import de.innologic.templateservice.domain.repository.TemplateVersionRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CachedTemplateLookupService {

    private final TemplateFamilyRepository templateFamilyRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final CacheManager cacheManager;

    public CachedTemplateLookupService(
            TemplateFamilyRepository templateFamilyRepository,
            TemplateVersionRepository templateVersionRepository,
            CacheManager cacheManager
    ) {
        this.templateFamilyRepository = templateFamilyRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.cacheManager = cacheManager;
    }

    @Cacheable(
            cacheNames = CacheConfig.TEMPLATE_RESOLVE_CACHE,
            key = "#scope + '|' + #ownerTenantId + '|' + #templateKey + '|' + #channel + '|' + #locale"
    )
    public Optional<ResolvedFamilyLookup> resolveTemplateFamily(
            TemplateScope scope,
            String ownerTenantId,
            String templateKey,
            String channel,
            String locale
    ) {
        return templateFamilyRepository
                .findFirstByScopeAndOwnerTenantIdAndTemplateKeyAndChannelAndLocale(scope, ownerTenantId, templateKey, channel, locale)
                .map(family -> new ResolvedFamilyLookup(family, locale));
    }

    @Cacheable(cacheNames = CacheConfig.TEMPLATE_APPROVED_VERSION_CACHE, key = "#templateId")
    public Optional<TemplateVersion> resolveApprovedVersion(UUID templateId, Integer activeApprovedVersion) {
        if (activeApprovedVersion != null) {
            Optional<TemplateVersion> active = templateVersionRepository.findByTemplateIdAndVersionNo(templateId, activeApprovedVersion);
            if (active.isPresent() && active.get().getStatus() == TemplateStatus.APPROVED) {
                return active;
            }
        }
        return templateVersionRepository.findFirstByTemplateIdAndStatusOrderByVersionNoDesc(templateId, TemplateStatus.APPROVED);
    }

    public void evictAll() {
        clear(CacheConfig.TEMPLATE_RESOLVE_CACHE);
        clear(CacheConfig.TEMPLATE_APPROVED_VERSION_CACHE);
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    public record ResolvedFamilyLookup(
            TemplateFamily family,
            String resolvedLocale
    ) {
    }
}
