package de.innologic.templateservice.api;

import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.domain.enums.TemplateStatus;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotFoundErrorCodeMockMvcIT extends AbstractMariaDbIntegrationTest {

    private static final String TENANT_ID = "tenant-a";
    private static final String ACTOR = "controller-test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TemplateTestFixture fixture;

    @BeforeEach
    void setUpJwt() {
        TestSecurityContext.setJwt(TENANT_ID, ACTOR, "template:read");
    }

    @AfterEach
    void clearJwt() {
        TestSecurityContext.clear();
    }

    @Test
    void unknownFamily_returnsTemplateNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/template/families/{templateId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TEMPLATE_NOT_FOUND"));
    }

    @Test
    void unknownVersion_returnsVersionNotFound() throws Exception {
        TemplateFamily family = fixture.createTenantFamily(TENANT_ID, "notfound.family");
        fixture.createVersion(family.getTemplateId(), 1, TemplateStatus.APPROVED, RenderTarget.TEXT, "Hello");

        mockMvc.perform(get("/api/v1/template/families/{templateId}/versions/{versionNo}", family.getTemplateId(), 999)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("VERSION_NOT_FOUND"));
    }
}
