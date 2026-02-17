package de.innologic.templateservice.security;

import de.innologic.templateservice.api.dto.PageResponse;
import de.innologic.templateservice.api.dto.CatalogTemplateFamilyResponse;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceServerSecurityIT {

    private static final String FAMILIES_ENDPOINT = "/api/v1/template/families";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private TemplateService templateService;

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get(FAMILIES_ENDPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void wrongAudience_returns401() throws Exception {
        when(jwtDecoder.decode("wrong-aud"))
                .thenThrow(new BadJwtException("Required audience is missing"));

        mockMvc.perform(get(FAMILIES_ENDPOINT)
                        .header("Authorization", "Bearer wrong-aud"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void missingTenantId_returns401() throws Exception {
        when(jwtDecoder.decode("missing-tenant"))
                .thenThrow(new BadJwtException("Missing required claim: tenant_id"));

        mockMvc.perform(get(FAMILIES_ENDPOINT)
                        .header("Authorization", "Bearer missing-tenant"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void missingScope_returns403() throws Exception {
        when(jwtDecoder.decode("no-scope")).thenReturn(jwtWithoutScope("no-scope"));

        mockMvc.perform(get(FAMILIES_ENDPOINT)
                        .header("Authorization", "Bearer no-scope"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void validReadScope_returns200() throws Exception {
        when(jwtDecoder.decode("valid-read")).thenReturn(jwtWithScope("valid-read", "template:read"));
        when(templateService.listFamilies(0, 50, "createdAt,DESC"))
                .thenReturn(new PageResponse<>(List.of(), 0, 50, 0));

        mockMvc.perform(get(FAMILIES_ENDPOINT)
                        .header("Authorization", "Bearer valid-read"))
                .andExpect(status().isOk());
    }

    @Test
    void tenantAdminWriteOnGlobalVersionEndpoint_returns403AccessDenied() throws Exception {
        String endpoint = "/api/v1/template/families/11111111-1111-1111-1111-111111111111/versions";
        when(jwtDecoder.decode("tenant-admin")).thenReturn(jwtWithScope("tenant-admin", "template:admin"));
        when(templateService.isGlobalFamily(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"))).thenReturn(true);

        mockMvc.perform(post(endpoint)
                        .header("Authorization", "Bearer tenant-admin")
                        .contentType("application/json")
                        .content("""
                            {
                              "status":"DRAFT",
                              "renderTarget":"TEXT",
                              "bodyTpl":"Hello {{name}}"
                            }
                            """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void catalogGlobal_asTenantWithReadScope_isAllowed() throws Exception {
        when(jwtDecoder.decode("read-catalog")).thenReturn(jwtWithScope("read-catalog", "template:read"));
        when(templateService.catalogFamilies(TemplateScope.GLOBAL, "EMAIL", "de-DE", 0, 50, "templateKey,ASC"))
                .thenReturn(new PageResponse<>(
                        List.of(new CatalogTemplateFamilyResponse(
                                java.util.UUID.randomUUID(), "email.confirmation", "EMAIL", "de-DE", "TRANSACTIONAL", 1
                        )),
                        0,
                        50,
                        1
                ));

        mockMvc.perform(get("/api/v1/template/catalog?scope=GLOBAL&channel=EMAIL&locale=de-DE&page=0&size=50&sort=templateKey,ASC")
                        .header("Authorization", "Bearer read-catalog"))
                .andExpect(status().isOk());
    }

    private Jwt jwtWithoutScope(String tokenValue) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .subject("test-user")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .audience(List.of("template-service"))
                .claim("tenant_id", "tenant-a")
                .build();
    }

    private Jwt jwtWithScope(String tokenValue, String scope) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .subject("test-user")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .audience(List.of("template-service"))
                .claim("tenant_id", "tenant-a")
                .claim("scope", scope)
                .build();
    }
}
