package de.innologic.templateservice.cache;

import com.github.benmanes.caffeine.cache.Cache;
import de.innologic.templateservice.api.dto.RenderRequest;
import de.innologic.templateservice.api.dto.RenderResponse;
import de.innologic.templateservice.api.error.NotFoundException;
import de.innologic.templateservice.config.CacheConfig;
import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.enums.MissingKeyPolicy;
import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import de.innologic.templateservice.service.TemplateService;
import de.innologic.templateservice.support.AbstractMariaDbIntegrationTest;
import de.innologic.templateservice.support.TemplateTestFixture;
import de.innologic.templateservice.support.TestSecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TemplateCachingIT extends AbstractMariaDbIntegrationTest {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TemplateTestFixture fixture;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetCaches() {
        cacheManager.getCache(CacheConfig.TEMPLATE_RESOLVE_CACHE).clear();
        cacheManager.getCache(CacheConfig.TEMPLATE_APPROVED_VERSION_CACHE).clear();
        TestSecurityContext.setJwt("tenant-a", "test-user", "template:read", "template:admin");
    }

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContext.clear();
    }

    @Test
    void resolveAndApprovedLookups_areCached_andApproveInvalidates() {
        TemplateFamily family = fixture.createTenantFamily("tenant-a", "email.confirmation");
        fixture.createVersion(family.getTemplateId(), 1, TemplateStatus.APPROVED, RenderTarget.TEXT, "Hello {{name}} v1");
        fixture.createVersion(family.getTemplateId(), 2, TemplateStatus.DRAFT, RenderTarget.TEXT, "Hello {{name}} v2");
        family.setActiveApprovedVersion(1);

        RenderRequest request = new RenderRequest(
                null,
                TemplateScope.TENANT,
                "email.confirmation",
                "EMAIL",
                "de-DE",
                null,
                MissingKeyPolicy.FAIL,
                Map.of("name", "Max"),
                null
        );

        RenderResponse first = templateService.renderApproved(request);
        RenderResponse second = templateService.renderApproved(request);
        assertThat(first.versionNo()).isEqualTo(1);
        assertThat(second.versionNo()).isEqualTo(1);

        Cache<Object, Object> resolveNative = nativeCaffeine(CacheConfig.TEMPLATE_RESOLVE_CACHE);
        Cache<Object, Object> approvedNative = nativeCaffeine(CacheConfig.TEMPLATE_APPROVED_VERSION_CACHE);
        assertThat(resolveNative.stats().hitCount()).isGreaterThan(0);
        assertThat(approvedNative.stats().hitCount()).isGreaterThan(0);

        templateService.approveVersion(family.getTemplateId(), 2);

        RenderResponse afterApprove = templateService.renderApproved(request);
        assertThat(afterApprove.versionNo()).isEqualTo(2);
    }

    @Test
    void renderApproved_wrongTenant_notFound() {
        TemplateFamily family = fixture.createTenantFamily("tenant-a", "tenant-blocked");
        fixture.createVersion(family.getTemplateId(), 1, TemplateStatus.APPROVED, RenderTarget.TEXT, "hello world");
        TestSecurityContext.setJwt("tenant-b", "intruder", "template:read");

        RenderRequest request = new RenderRequest(
                null,
                TemplateScope.TENANT,
                "tenant-blocked",
                "EMAIL",
                "de-DE",
                null,
                MissingKeyPolicy.FAIL,
                Map.of("name", "Max"),
                null
        );

        assertThatThrownBy(() -> templateService.renderApproved(request))
                .isInstanceOf(NotFoundException.class);
    }

    @SuppressWarnings("unchecked")
    private Cache<Object, Object> nativeCaffeine(String cacheName) {
        org.springframework.cache.caffeine.CaffeineCache springCache =
                (org.springframework.cache.caffeine.CaffeineCache) cacheManager.getCache(cacheName);
        return (Cache<Object, Object>) springCache.getNativeCache();
    }
}
