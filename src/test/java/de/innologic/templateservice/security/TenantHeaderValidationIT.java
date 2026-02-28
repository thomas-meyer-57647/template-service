package de.innologic.templateservice.security;

import de.innologic.templateservice.support.AbstractMariaDbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantHeaderValidationIT extends AbstractMariaDbIntegrationTest {

    private static final String FAMILIES_ENDPOINT = "/api/v1/template/families";
    private static final String HEADER = "X-Tenant-Id";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void matchingHeader_permitsAccess() throws Exception {
        mockMvc.perform(get(FAMILIES_ENDPOINT)
                        .with(jwtWithTenant("tenant-a"))
                        .header(HEADER, "tenant-a")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void mismatchedHeader_blocksAccess() throws Exception {
        mockMvc.perform(get(FAMILIES_ENDPOINT)
                        .with(jwtWithTenant("tenant-a"))
                        .header(HEADER, "tenant-b")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("TENANT_MISMATCH"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    private static JwtRequestPostProcessor jwtWithTenant(String tenantId) {
        return jwt()
                .jwt(jwt -> jwt.claim("tenant_id", tenantId)
                        .claim("scope", "template:read"));
    }
}
