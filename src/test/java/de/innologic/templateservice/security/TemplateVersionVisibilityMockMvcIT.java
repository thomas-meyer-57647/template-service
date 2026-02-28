package de.innologic.templateservice.security;

import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.enums.RenderTarget;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TemplateVersionVisibilityMockMvcIT extends AbstractMariaDbIntegrationTest {

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TemplateTestFixture fixture;

    private TemplateFamily tenantAFamily;
    private TemplateFamily tenantBFamily;

    @BeforeEach
    void setUp() {
        tenantAFamily = fixture.createTenantFamily(TENANT_A, "visible.family");
        fixture.createVersion(tenantAFamily.getTemplateId(), 1, TemplateStatus.APPROVED, RenderTarget.TEXT, "body");

        tenantBFamily = fixture.createTenantFamily(TENANT_B, "hidden.family");
        fixture.createVersion(tenantBFamily.getTemplateId(), 1, TemplateStatus.APPROVED, RenderTarget.TEXT, "body");

        TestSecurityContext.setJwt(TENANT_A, "version-visibility", "template:read");
    }

    @AfterEach
    void tearDown() {
        TestSecurityContext.clear();
    }

    @Test
    void tenantCanFetchOwnVersion() throws Exception {
        mockMvc.perform(get("/api/v1/template/families/{templateId}/versions/{versionNo}",
                        tenantAFamily.getTemplateId(), 1)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void tenantCannotFetchOtherTenantVersion() throws Exception {
        mockMvc.perform(get("/api/v1/template/families/{templateId}/versions/{versionNo}",
                        tenantBFamily.getTemplateId(), 1)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TEMPLATE_NOT_FOUND"));
    }
}
