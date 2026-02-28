package de.innologic.templateservice.api;

import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.support.AbstractMariaDbIntegrationTest;
import de.innologic.templateservice.support.TemplateTestFixture;
import de.innologic.templateservice.support.TestSecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TemplateVersionAuditMockMvcIT extends AbstractMariaDbIntegrationTest {

    private static final String TENANT_ID = "tenant-a";
    private static final String ACTOR = "audit-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TemplateTestFixture fixture;

    private TemplateFamily tenantFamily;

    @BeforeEach
    void setUp() {
        tenantFamily = fixture.createTenantFamily(TENANT_ID, "audit.family");
        TestSecurityContext.setJwt(TENANT_ID, ACTOR, "template:admin", "template:read");
    }

    @AfterEach
    void tearDown() {
        TestSecurityContext.clear();
    }

    @Test
    void createVersion_setsAuditFieldsFromJwt() throws Exception {
        String payload = """
            {
              "renderTarget": "TEXT",
              "subjectTpl": "Hi {{name}}",
              "bodyTpl": "Hello {{name}}",
              "placeholders": "[]"
            }
            """;

        mockMvc.perform(post("/api/v1/template/families/{templateId}/versions", tenantFamily.getTemplateId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value(ACTOR))
                .andExpect(jsonPath("$.updatedBy").value(ACTOR))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createVersion_missingBodyTpl_returnsInvalidRequest() throws Exception {
        String payload = """
            {
              "renderTarget": "TEXT",
              "subjectTpl": "Hi {{name}}"
            }
            """;

        mockMvc.perform(post("/api/v1/template/families/{templateId}/versions", tenantFamily.getTemplateId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }
}
