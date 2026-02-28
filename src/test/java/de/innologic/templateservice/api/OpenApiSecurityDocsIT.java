package de.innologic.templateservice.api;

import de.innologic.templateservice.support.AbstractMariaDbIntegrationTest;
import de.innologic.templateservice.support.TestSecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSecurityDocsIT extends AbstractMariaDbIntegrationTest {

    private static final String TENANT_ID = "tenant-docs";
    private static final String ACTOR = "docs";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUpJwt() {
        TestSecurityContext.setJwt(TENANT_ID, ACTOR, "template:read");
    }

    @AfterEach
    void tearDownJwt() {
        TestSecurityContext.clear();
    }

    @Test
    void apiDocsExposeBearerSecurityScheme() throws Exception {
        mockMvc.perform(get("/api/v1/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void apiDocsIncludeSecurityRequirementForFamiliesEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/template/families'].get.security[0].bearerAuth").isArray());
    }
}
