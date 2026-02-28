package de.innologic.templateservice.security;

import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.domain.repository.TemplateFamilyRepository;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TemplateVisibilityMockMvcIT extends AbstractMariaDbIntegrationTest {

    private static final String GLOBAL_OWNER = "__GLOBAL__";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TemplateFamilyRepository familyRepository;

    @Autowired
    private TemplateTestFixture fixture;

    private TemplateFamily tenantAFamily;
    private TemplateFamily tenantBFamily;

    @BeforeEach
    void setUp() {
        familyRepository.deleteAllInBatch();

        TemplateFamily globalFamily = new TemplateFamily();
        globalFamily.setTemplateId(UUID.randomUUID());
        globalFamily.setScope(TemplateScope.GLOBAL);
        globalFamily.setOwnerTenantId(GLOBAL_OWNER);
        globalFamily.setTemplateKey("global.template");
        globalFamily.setChannel("EMAIL");
        globalFamily.setLocale("de-DE");
        globalFamily.setCategory("SYSTEM");
        globalFamily.setCreatedBy("test");
        globalFamily.setUpdatedBy("test");
        familyRepository.save(globalFamily);

        tenantAFamily = fixture.createTenantFamily("tenant-a", "tenant.a.template");
        tenantBFamily = fixture.createTenantFamily("tenant-b", "tenant.b.template");

        TestSecurityContext.setJwt("tenant-a", "visibility-tests", "template:read");
    }

    @AfterEach
    void tearDown() {
        TestSecurityContext.clear();
    }

    @Test
    void tenantSeesGlobalAndOwnFamily() throws Exception {
        mockMvc.perform(get("/api/v1/template/families").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].ownerTenantId", containsInAnyOrder(GLOBAL_OWNER, "tenant-a")))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void tenantCannotAccessOtherTenantFamily() throws Exception {
        mockMvc.perform(get("/api/v1/template/families/{templateId}", tenantBFamily.getTemplateId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TEMPLATE_NOT_FOUND"));
    }
}
