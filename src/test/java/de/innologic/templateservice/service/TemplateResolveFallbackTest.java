package de.innologic.templateservice.service;

import tools.jackson.databind.json.JsonMapper;
import de.innologic.templateservice.api.dto.RenderRequest;
import de.innologic.templateservice.api.dto.RenderResponse;
import de.innologic.templateservice.api.error.NotFoundException;
import de.innologic.templateservice.api.error.UnprocessableTemplateException;
import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.entity.TemplateVersion;
import de.innologic.templateservice.domain.enums.MissingKeyPolicy;
import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import de.innologic.templateservice.domain.repository.TemplateFamilyRepository;
import de.innologic.templateservice.domain.repository.TemplateVersionRepository;
import de.innologic.templateservice.events.TemplateEventPublisher;
import de.innologic.templateservice.security.TenantContext;
import de.innologic.templateservice.security.TenantContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateResolveFallbackTest {

    @Mock
    private TemplateFamilyRepository templateFamilyRepository;

    @Mock
    private TemplateVersionRepository templateVersionRepository;

    @Mock
    private TenantContextResolver tenantContextResolver;

    @Mock
    private CachedTemplateLookupService cachedTemplateLookupService;

    @Mock
    private TemplateEventPublisher templateEventPublisher;

    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(
                templateFamilyRepository,
                templateVersionRepository,
                new JsonMapper(),
                tenantContextResolver,
                cachedTemplateLookupService,
                templateEventPublisher,
                "en-GB"
        );
        when(tenantContextResolver.resolveRequired()).thenReturn(new TenantContext("tenant-a", "user-a"));
    }

    @Test
    void render_exactLocaleHit_usesExactLocale() {
        TemplateFamily family = tenantFamily("de-DE");
        stubApprovedVersion(family);
        when(cachedTemplateLookupService.resolveTemplateFamily(
                TemplateScope.TENANT, "tenant-a", "email.confirmation", "EMAIL", "de-DE"))
                .thenReturn(Optional.of(new CachedTemplateLookupService.ResolvedFamilyLookup(family, "de-DE")));
        when(cachedTemplateLookupService.resolveApprovedVersion(family.getTemplateId(), 1))
                .thenReturn(Optional.of(approvedVersion(family, RenderTarget.TEXT, "Hallo {{firstName}}")));

        RenderResponse response = templateService.renderApproved(request("de-DE"));

        assertThat(response.resolvedLocale()).isEqualTo("de-DE");
        assertThat(response.renderedBody()).isEqualTo("Hallo Max");
    }

    @Test
    void render_fallbackToLanguage_usesLanguageLocale() {
        TemplateFamily family = tenantFamily("de");
        stubApprovedVersion(family);
        when(cachedTemplateLookupService.resolveTemplateFamily(
                TemplateScope.TENANT, "tenant-a", "email.confirmation", "EMAIL", "de-DE"))
                .thenReturn(Optional.empty());
        when(cachedTemplateLookupService.resolveTemplateFamily(
                TemplateScope.TENANT, "tenant-a", "email.confirmation", "EMAIL", "de"))
                .thenReturn(Optional.of(new CachedTemplateLookupService.ResolvedFamilyLookup(family, "de")));

        RenderResponse response = templateService.renderApproved(request("de-DE"));

        assertThat(response.resolvedLocale()).isEqualTo("de");
        assertThat(response.renderedBody()).isEqualTo("Hallo Max");
    }

    @Test
    void render_fallbackToDefaultLocale_usesConfiguredDefault() {
        TemplateFamily family = tenantFamily("en-GB");
        stubApprovedVersion(family);
        when(cachedTemplateLookupService.resolveTemplateFamily(
                TemplateScope.TENANT, "tenant-a", "email.confirmation", "EMAIL", "fr-CA"))
                .thenReturn(Optional.empty());
        when(cachedTemplateLookupService.resolveTemplateFamily(
                TemplateScope.TENANT, "tenant-a", "email.confirmation", "EMAIL", "fr"))
                .thenReturn(Optional.empty());
        when(cachedTemplateLookupService.resolveTemplateFamily(
                TemplateScope.TENANT, "tenant-a", "email.confirmation", "EMAIL", "en-GB"))
                .thenReturn(Optional.of(new CachedTemplateLookupService.ResolvedFamilyLookup(family, "en-GB")));

        RenderResponse response = templateService.renderApproved(request("fr-CA"));

        assertThat(response.resolvedLocale()).isEqualTo("en-GB");
    }

    @Test
    void render_noLocaleMatch_throwsTemplateNotFound() {
        when(cachedTemplateLookupService.resolveTemplateFamily(
                eq(TemplateScope.TENANT), eq("tenant-a"), eq("email.confirmation"), eq("EMAIL"), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.renderApproved(request("fr-CA")))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> {
                    assertThat(ex).hasMessageStartingWith("Template family not found");
                    assertThat(((NotFoundException) ex).errorCode()).isEqualTo("TEMPLATE_NOT_FOUND");
                });
    }

    @Test
    void preview_archivedVersion_isNotRenderable() {
        TemplateFamily family = tenantFamily("de-DE");
        TemplateVersion archived = new TemplateVersion();
        archived.setVersionId(UUID.randomUUID());
        archived.setTemplateId(family.getTemplateId());
        archived.setVersionNo(4);
        archived.setStatus(TemplateStatus.ARCHIVED);
        archived.setRenderTarget(RenderTarget.TEXT);
        archived.setBodyTpl("archived");

        when(cachedTemplateLookupService.resolveTemplateFamily(
                TemplateScope.TENANT, "tenant-a", "email.confirmation", "EMAIL", "de-DE"))
                .thenReturn(Optional.of(new CachedTemplateLookupService.ResolvedFamilyLookup(family, "de-DE")));
        when(templateVersionRepository.findFirstByTemplateIdOrderByVersionNoDesc(family.getTemplateId()))
                .thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> templateService.preview(request("de-DE")))
                .isInstanceOf(UnprocessableTemplateException.class)
                .hasMessage("VERSION_NOT_RENDERABLE");
    }

    @Test
    void render_missingKeyPolicyFail_returnsMissingKeysError() {
        TemplateFamily family = tenantFamily("de-DE");
        TemplateVersion version = approvedVersion(family, RenderTarget.TEXT, "Hi {{firstName}} {{lastName}}");
        when(cachedTemplateLookupService.resolveTemplateFamily(
                TemplateScope.TENANT, "tenant-a", "email.confirmation", "EMAIL", "de-DE"))
                .thenReturn(Optional.of(new CachedTemplateLookupService.ResolvedFamilyLookup(family, "de-DE")));
        when(cachedTemplateLookupService.resolveApprovedVersion(family.getTemplateId(), 1))
                .thenReturn(Optional.of(version));

        RenderRequest request = new RenderRequest(
                null, TemplateScope.TENANT, "email.confirmation", "EMAIL", "de-DE", null,
                MissingKeyPolicy.FAIL, Map.of("firstName", "Max"), null
        );

        assertThatThrownBy(() -> templateService.renderApproved(request))
                .isInstanceOf(UnprocessableTemplateException.class)
                .hasMessage("MISSING_KEYS");
    }

    @Test
    void render_missingKeyPolicyKeepToken_keepsTokenAndWarning() {
        TemplateFamily family = tenantFamily("de-DE");
        TemplateVersion version = approvedVersion(family, RenderTarget.TEXT, "Hi {{firstName}} {{lastName}}");
        when(cachedTemplateLookupService.resolveTemplateFamily(
                TemplateScope.TENANT, "tenant-a", "email.confirmation", "EMAIL", "de-DE"))
                .thenReturn(Optional.of(new CachedTemplateLookupService.ResolvedFamilyLookup(family, "de-DE")));
        when(cachedTemplateLookupService.resolveApprovedVersion(family.getTemplateId(), 1))
                .thenReturn(Optional.of(version));

        RenderRequest request = new RenderRequest(
                null, TemplateScope.TENANT, "email.confirmation", "EMAIL", "de-DE", null,
                MissingKeyPolicy.KEEP_TOKEN, Map.of("firstName", "Max"), null
        );

        RenderResponse response = templateService.renderApproved(request);

        assertThat(response.renderedBody()).isEqualTo("Hi Max {{lastName}}");
        assertThat(response.warnings()).isNotEmpty();
    }

    @Test
    void render_missingKeyPolicyEmpty_replacesWithEmptyAndWarning() {
        TemplateFamily family = tenantFamily("de-DE");
        TemplateVersion version = approvedVersion(family, RenderTarget.TEXT, "Hi {{firstName}} {{lastName}}");
        when(cachedTemplateLookupService.resolveTemplateFamily(
                TemplateScope.TENANT, "tenant-a", "email.confirmation", "EMAIL", "de-DE"))
                .thenReturn(Optional.of(new CachedTemplateLookupService.ResolvedFamilyLookup(family, "de-DE")));
        when(cachedTemplateLookupService.resolveApprovedVersion(family.getTemplateId(), 1))
                .thenReturn(Optional.of(version));

        RenderRequest request = new RenderRequest(
                null, TemplateScope.TENANT, "email.confirmation", "EMAIL", "de-DE", null,
                MissingKeyPolicy.EMPTY, Map.of("firstName", "Max"), null
        );

        RenderResponse response = templateService.renderApproved(request);

        assertThat(response.renderedBody()).isEqualTo("Hi Max ");
        assertThat(response.warnings()).isNotEmpty();
    }

    @Test
    void render_htmlEscapesInjectedValueByDefault() {
        TemplateFamily family = tenantFamily("de-DE");
        TemplateVersion version = approvedVersion(family, RenderTarget.HTML, "<p>{{payload}}</p>");
        when(cachedTemplateLookupService.resolveTemplateFamily(
                TemplateScope.TENANT, "tenant-a", "email.confirmation", "EMAIL", "de-DE"))
                .thenReturn(Optional.of(new CachedTemplateLookupService.ResolvedFamilyLookup(family, "de-DE")));
        when(cachedTemplateLookupService.resolveApprovedVersion(family.getTemplateId(), 1))
                .thenReturn(Optional.of(version));

        RenderRequest request = new RenderRequest(
                null, TemplateScope.TENANT, "email.confirmation", "EMAIL", "de-DE", null,
                MissingKeyPolicy.FAIL, Map.of("payload", "<script>alert(1)</script>"), null
        );

        RenderResponse response = templateService.renderApproved(request);

        assertThat(response.contentType()).isEqualTo("text/html");
        assertThat(response.renderedBody()).isEqualTo("<p>&lt;script&gt;alert(1)&lt;/script&gt;</p>");
    }

    private RenderRequest request(String locale) {
        return new RenderRequest(
                null,
                TemplateScope.TENANT,
                "email.confirmation",
                "EMAIL",
                locale,
                null,
                MissingKeyPolicy.FAIL,
                Map.of("firstName", "Max"),
                null
        );
    }

    private TemplateFamily tenantFamily(String locale) {
        TemplateFamily family = new TemplateFamily();
        family.setTemplateId(UUID.randomUUID());
        family.setScope(TemplateScope.TENANT);
        family.setOwnerTenantId("tenant-a");
        family.setTemplateKey("email.confirmation");
        family.setChannel("EMAIL");
        family.setLocale(locale);
        family.setActiveApprovedVersion(1);
        return family;
    }

    private void stubApprovedVersion(TemplateFamily family) {
        TemplateVersion version = approvedVersion(family, RenderTarget.TEXT, "Hallo {{firstName}}");
        when(cachedTemplateLookupService.resolveApprovedVersion(family.getTemplateId(), 1))
                .thenReturn(Optional.of(version));
    }

    private TemplateVersion approvedVersion(TemplateFamily family, RenderTarget target, String bodyTpl) {
        TemplateVersion version = new TemplateVersion();
        version.setVersionId(UUID.randomUUID());
        version.setTemplateId(family.getTemplateId());
        version.setVersionNo(1);
        version.setStatus(TemplateStatus.APPROVED);
        version.setRenderTarget(target);
        version.setBodyTpl(bodyTpl);
        return version;
    }
}
