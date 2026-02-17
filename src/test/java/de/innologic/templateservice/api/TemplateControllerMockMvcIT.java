package de.innologic.templateservice.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.templateservice.api.dto.RenderResponse;
import de.innologic.templateservice.api.dto.PageResponse;
import de.innologic.templateservice.api.dto.TemplateFamilyResponse;
import de.innologic.templateservice.api.dto.TemplateVersionResponse;
import de.innologic.templateservice.api.dto.ValidateTemplateResponse;
import de.innologic.templateservice.api.dto.CatalogTemplateFamilyResponse;
import de.innologic.templateservice.api.dto.CatalogTemplateVersionResponse;
import de.innologic.templateservice.api.error.ConflictException;
import de.innologic.templateservice.api.error.UnprocessableTemplateException;
import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import de.innologic.templateservice.service.TemplateService;
import de.innologic.templateservice.support.TestControllerExceptionAdvice;
import de.innologic.templateservice.support.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static de.innologic.templateservice.support.TestJwtFactory.jwtWithTenantAndRoles;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestControllerExceptionAdvice.class})
class TemplateControllerMockMvcIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private TemplateService templateService;

    @Test
    void tenantAdmin_canCreateAndReadTenantTemplateFamily() throws Exception {
        // Arrange
        UUID templateId = UUID.randomUUID();
        TemplateFamilyResponse created = familyResponse(templateId, TemplateScope.TENANT, "tenantA", 1);
        when(templateService.createFamily(any())).thenReturn(created);
        when(templateService.getFamily(eq(templateId))).thenReturn(created);

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/families")
                .with(jwtWithTenantAndRoles("tenantA", "tenant_admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scope":"TENANT",
                      "ownerTenantId":"tenantA",
                      "templateKey":"invoice.reminder",
                      "channel":"EMAIL",
                      "locale":"de-DE",
                      "category":"BILLING",
                      "updatedBy":"tenant-admin-a"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.templateId").value(templateId.toString()))
            .andExpect(jsonPath("$.scope").value("TENANT"))
            .andExpect(jsonPath("$.ownerTenantId").value("tenantA"));

        mockMvc.perform(get("/api/v1/template/families/{templateId}", templateId)
                .with(jwtWithTenantAndRoles("tenantA", "tenant_admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.templateId").value(templateId.toString()))
            .andExpect(jsonPath("$.templateKey").value("invoice.reminder"));
    }

    @Test
    void listFamilies_returnsPagingMetadata() throws Exception {
        UUID templateId = UUID.randomUUID();
        TemplateFamilyResponse item = familyResponse(templateId, TemplateScope.TENANT, "tenantA", 1);
        when(templateService.listFamilies(0, 2, "createdAt,DESC"))
                .thenReturn(new PageResponse<>(List.of(item), 0, 2, 1));

        mockMvc.perform(get("/api/v1/template/families?page=0&size=2&sort=createdAt,DESC")
                        .with(jwtWithTenantAndRoles("tenantA", "tenant_admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].templateId").value(templateId.toString()));
    }

    @Test
    void catalogList_returnsMetadataAndPaging() throws Exception {
        UUID templateId = UUID.randomUUID();
        CatalogTemplateFamilyResponse item = new CatalogTemplateFamilyResponse(
                templateId, "email.confirmation", "EMAIL", "de-DE", "TRANSACTIONAL", 3
        );
        when(templateService.catalogFamilies(TemplateScope.GLOBAL, "EMAIL", "de-DE", 0, 50, "templateKey,ASC"))
                .thenReturn(new PageResponse<>(List.of(item), 0, 50, 1));

        mockMvc.perform(get("/api/v1/template/catalog?scope=GLOBAL&channel=EMAIL&locale=de-DE&page=0&size=50&sort=templateKey,ASC")
                        .with(jwtWithTenantAndRoles("tenantA", "tenant_admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].templateId").value(templateId.toString()))
                .andExpect(jsonPath("$.items[0].templateKey").value("email.confirmation"))
                .andExpect(jsonPath("$.items[0].bodyTpl").doesNotExist());
    }

    @Test
    void catalogApprovedVersions_returnsOnlyApprovedMetadata() throws Exception {
        UUID templateId = UUID.randomUUID();
        CatalogTemplateVersionResponse approved = new CatalogTemplateVersionResponse(
                UUID.randomUUID(), templateId, 3, TemplateStatus.APPROVED, RenderTarget.HTML, Instant.parse("2026-02-17T09:00:00Z")
        );
        when(templateService.catalogApprovedVersions(templateId, 0, 50, "versionNo,DESC"))
                .thenReturn(new PageResponse<>(List.of(approved), 0, 50, 1));

        mockMvc.perform(get("/api/v1/template/catalog/{templateId}/approved-versions?page=0&size=50&sort=versionNo,DESC", templateId)
                        .with(jwtWithTenantAndRoles("tenantA", "tenant_admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.items[0].bodyTpl").doesNotExist());
    }

    @Test
    void tenantAdmin_canCreateDraftVersionAndApprove_activeApprovedVersionSet() throws Exception {
        // Arrange
        UUID templateId = UUID.randomUUID();
        TemplateVersionResponse draft = versionResponse(templateId, 1, TemplateStatus.DRAFT, RenderTarget.TEXT, "Hello {{name}}");
        TemplateVersionResponse approved = versionResponse(templateId, 1, TemplateStatus.APPROVED, RenderTarget.TEXT, "Hello {{name}}");
        TemplateFamilyResponse familyAfterApprove = familyResponse(templateId, TemplateScope.TENANT, "tenantA", 1);

        when(templateService.createVersion(eq(templateId), any())).thenReturn(draft);
        when(templateService.approveVersion(eq(templateId), eq(1), any())).thenReturn(approved);
        when(templateService.getFamily(eq(templateId))).thenReturn(familyAfterApprove);

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/families/{templateId}/versions", templateId)
                .with(jwtWithTenantAndRoles("tenantA", "tenant_admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status":"DRAFT",
                      "renderTarget":"TEXT",
                      "bodyTpl":"Hello {{name}}",
                      "placeholders":"[\\"name\\"]",
                      "updatedBy":"tenant-admin-a"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(post("/api/v1/template/families/{templateId}/versions/{versionNo}/approve", templateId, 1)
                .with(jwtWithTenantAndRoles("tenantA", "tenant_admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"updatedBy\":\"tenant-admin-a\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/v1/template/families/{templateId}", templateId)
                .with(jwtWithTenantAndRoles("tenantA", "tenant_admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeApprovedVersion").value(1));
    }

    @Test
    void renderApproved_text_replacesPlaceholders() throws Exception {
        // Arrange
        UUID templateId = UUID.randomUUID();
        RenderResponse response = new RenderResponse(
            TemplateScope.TENANT, templateId, "de-DE", 2, TemplateStatus.APPROVED, RenderTarget.TEXT, "text/plain", "Hi Max", "Hello Max", List.of()
        );
        when(templateService.renderApproved(any())).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/render")
                .with(jwtWithTenantAndRoles("tenantA", "user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateId":"%s",
                      "locale":"de-DE",
                      "variables":{"name":"Max"}
                    }
                    """.formatted(templateId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.renderedBody").value("Hello Max"));
    }

    @Test
    void renderApproved_html_escapesContent() throws Exception {
        // Arrange
        UUID templateId = UUID.randomUUID();
        RenderResponse response = new RenderResponse(
            TemplateScope.TENANT,
            templateId,
            "de-DE",
            1,
            TemplateStatus.APPROVED,
            RenderTarget.HTML,
            "text/html",
            null,
            "<p>&lt;script&gt;alert(1)&lt;/script&gt;</p>",
            List.of()
        );
        when(templateService.renderApproved(any())).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/render")
                .with(jwtWithTenantAndRoles("tenantA", "user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateId":"%s",
                      "locale":"de-DE",
                      "variables":{"unsafe":"<script>alert(1)</script>"}
                    }
                    """.formatted(templateId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.renderTarget").value("HTML"))
            .andExpect(jsonPath("$.renderedBody").value("<p>&lt;script&gt;alert(1)&lt;/script&gt;</p>"));
    }

    @Test
    void preview_rendersDraft() throws Exception {
        // Arrange
        UUID templateId = UUID.randomUUID();
        RenderResponse response = new RenderResponse(
            TemplateScope.TENANT, templateId, "de-DE", 3, TemplateStatus.DRAFT, RenderTarget.TEXT, "text/plain", null, "Preview for Draft", List.of()
        );
        when(templateService.preview(any())).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/preview")
                .with(jwtWithTenantAndRoles("tenantA", "tenant_admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateId":"%s",
                      "locale":"de-DE",
                      "versionNo":3,
                      "variables":{"name":"Max"}
                    }
                    """.formatted(templateId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.renderedBody").value("Preview for Draft"));
    }

    @Test
    void validate_returnsDetectedPlaceholders_withoutErrors() throws Exception {
        // Arrange
        ValidateTemplateResponse response = new ValidateTemplateResponse(
            true,
            List.of("customerName", "invoiceNo"),
            List.of("customerName", "invoiceNo"),
            List.of()
        );
        when(templateService.validate(any())).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/validate")
                .with(jwtWithTenantAndRoles("tenantA", "user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "subjectTpl":"Hallo {{customerName}}",
                      "bodyTpl":"Rechnung {{invoiceNo}} für {{customerName}}",
                      "placeholders":"[\\"customerName\\",\\"invoiceNo\\"]"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.detectedPlaceholders[0]").value("customerName"))
            .andExpect(jsonPath("$.errors", empty()));
    }

    @Test
    void requestWithoutJwt_returns401Unauthenticated() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(post("/api/v1/template/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"bodyTpl":"Hello {{name}}","placeholders":"[\\"name\\"]"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void tenantAdmin_creatingGlobalTemplate_returns403Forbidden() throws Exception {
        // Arrange
        when(templateService.createFamily(any()))
            .thenThrow(new AccessDeniedException("GLOBAL templates require platform_admin"));

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/families")
                .with(jwtWithTenantAndRoles("tenantA", "tenant_admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scope":"GLOBAL",
                      "templateKey":"system.alert",
                      "channel":"EMAIL",
                      "locale":"de-DE",
                      "category":"SYSTEM"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void tenantB_readingTenantATemplate_returnsForbiddenWithErrorDto() throws Exception {
        // Arrange
        UUID templateId = UUID.randomUUID();
        when(templateService.getFamily(eq(templateId)))
            .thenThrow(new AccessDeniedException("Template does not belong to tenantB"));

        // Act + Assert
        mockMvc.perform(get("/api/v1/template/families/{templateId}", templateId)
                .with(jwtWithTenantAndRoles("tenantB", "tenant_admin")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("Template does not belong to tenantB"));
    }

    @Test
    void renderDraftOnProductiveEndpoint_returnsVersionNotRenderable() throws Exception {
        // Arrange
        when(templateService.renderApproved(any()))
            .thenThrow(new UnprocessableTemplateException("VERSION_NOT_RENDERABLE"));

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/render")
                .with(jwtWithTenantAndRoles("tenantA", "user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateId":"%s",
                      "locale":"de-DE",
                      "versionNo":1,
                      "variables":{"name":"Max"}
                    }
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode").value("VERSION_NOT_RENDERABLE"));
    }

    @Test
    void missingKeyPolicyFail_returns422AndMissingKeysDetails() throws Exception {
        // Arrange
        when(templateService.renderApproved(any()))
            .thenThrow(new UnprocessableTemplateException("MISSING_KEYS missingKeys=[customerName,invoiceNo]"));

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/render")
                .with(jwtWithTenantAndRoles("tenantA", "user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateId":"%s",
                      "locale":"de-DE",
                      "variables":{"customerName":"Max"}
                    }
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode").value("MISSING_KEYS"))
            .andExpect(jsonPath("$.details.missingKeys", not(empty())));
    }

    @Test
    void tenantShadowingGlobalTemplate_returns409TemplateKeyReserved() throws Exception {
        // Arrange
        when(templateService.createFamily(any()))
            .thenThrow(new ConflictException("TEMPLATE_SHADOWING_FORBIDDEN"));

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/families")
                .with(jwtWithTenantAndRoles("tenantA", "tenant_admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scope":"TENANT",
                      "ownerTenantId":"tenantA",
                      "templateKey":"system.reset-password",
                      "channel":"EMAIL",
                      "locale":"de-DE",
                      "category":"SYSTEM"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("TEMPLATE_SHADOWING_FORBIDDEN"));
    }

    @Test
    void tenantCreateReservedKey_returns422TemplateKeyReserved() throws Exception {
        // Arrange
        when(templateService.createFamily(any()))
            .thenThrow(new UnprocessableTemplateException("TEMPLATE_KEY_RESERVED"));

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/families")
                .with(jwtWithTenantAndRoles("tenantA", "tenant_admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scope":"TENANT",
                      "ownerTenantId":"tenantA",
                      "templateKey":"platform.welcome",
                      "channel":"EMAIL",
                      "locale":"de-DE",
                      "category":"SYSTEM"
                    }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode").value("TEMPLATE_KEY_RESERVED"));
    }

    @Test
    void templateSyntaxError_returns422TemplateSyntaxError() throws Exception {
        // Arrange
        when(templateService.validate(any()))
            .thenThrow(new UnprocessableTemplateException("TEMPLATE_SYNTAX_ERROR: unmatched token '{{'"));

        // Act + Assert
        mockMvc.perform(post("/api/v1/template/validate")
                .with(jwtWithTenantAndRoles("tenantA", "user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "bodyTpl":"Hello {{name",
                      "placeholders":"[\\"name\\"]"
                    }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode").value("TEMPLATE_SYNTAX_ERROR"));
    }

    @Test
    void updateExistingVersionEndpoint_returns409VersionImmutable() throws Exception {
        // Arrange
        UUID templateId = UUID.randomUUID();
        when(templateService.updateVersion(eq(templateId), eq(2), any()))
            .thenThrow(new ConflictException("VERSION_IMMUTABLE"));

        // Act + Assert
        mockMvc.perform(put("/api/v1/template/families/{templateId}/versions/{versionNo}", templateId, 2)
                .with(jwtWithTenantAndRoles("tenantA", "tenant_admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status":"DRAFT",
                      "renderTarget":"TEXT",
                      "bodyTpl":"Hello {{name}}",
                      "placeholders":"[\\"name\\"]"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("VERSION_IMMUTABLE"));
    }

    private static TemplateFamilyResponse familyResponse(
        UUID templateId,
        TemplateScope scope,
        String tenantId,
        Integer activeApprovedVersion
    ) {
        Instant now = Instant.parse("2026-02-15T15:00:00Z");
        return new TemplateFamilyResponse(
            templateId,
            scope,
            tenantId,
            "invoice.reminder",
            "EMAIL",
            "de-DE",
            "BILLING",
            activeApprovedVersion,
            now,
            "test",
            now,
            "test"
        );
    }

    private static TemplateVersionResponse versionResponse(
        UUID templateId,
        Integer versionNo,
        TemplateStatus status,
        RenderTarget renderTarget,
        String bodyTpl
    ) {
        Instant now = Instant.parse("2026-02-15T15:00:00Z");
        return new TemplateVersionResponse(
            UUID.randomUUID(),
            templateId,
            versionNo,
            status,
            renderTarget,
            "Subject {{name}}",
            bodyTpl,
            "[\"name\"]",
            now,
            "test",
            now,
            "test"
        );
    }
}
