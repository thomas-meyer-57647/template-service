package de.innologic.templateservice.persistence;

import de.innologic.templateservice.api.dto.TemplateVersionResponse;
import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.entity.TemplateVersion;
import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import de.innologic.templateservice.domain.repository.TemplateFamilyRepository;
import de.innologic.templateservice.domain.repository.TemplateVersionRepository;
import de.innologic.templateservice.service.TemplateService;
import de.innologic.templateservice.support.AbstractMariaDbIntegrationTest;
import de.innologic.templateservice.support.TemplateTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TemplatePersistenceMariaDbIT extends AbstractMariaDbIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TemplateFamilyRepository familyRepository;

    @Autowired
    private TemplateVersionRepository versionRepository;

    @Autowired
    private TemplateTestFixture fixture;

    @Autowired
    private TemplateService templateService;

    @BeforeEach
    void cleanDatabase() {
        versionRepository.deleteAll();
        familyRepository.deleteAll();
    }

    @Test
    void flyway_shouldCreateTemplateTablesAndIndexes() {
        // Arrange + Act
        Integer familyTableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'template_family'",
            Integer.class
        );
        Integer versionTableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'template_version'",
            Integer.class
        );
        Integer familyIndexCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name='template_family' AND index_name='idx_template_family_lookup'",
            Integer.class
        );

        // Assert
        assertThat(familyTableCount).isEqualTo(1);
        assertThat(versionTableCount).isEqualTo(1);
        assertThat(familyIndexCount).isEqualTo(1);
    }

    @Test
    void repositories_shouldPersistAndLoadFamilyAndVersions() {
        // Arrange
        TemplateFamily family = fixture.createTenantFamily("tenantA", "invoice.reminder");
        TemplateVersion v1 = fixture.createVersion(family.getTemplateId(), 1, TemplateStatus.DRAFT, RenderTarget.TEXT, "Hello {{name}}");
        TemplateVersion v2 = fixture.createVersion(family.getTemplateId(), 2, TemplateStatus.APPROVED, RenderTarget.TEXT, "Hello approved {{name}}");

        // Act
        TemplateFamily reloadedFamily = familyRepository.findById(family.getTemplateId()).orElseThrow();
        TemplateVersion reloadedV2 = versionRepository.findByTemplateIdAndVersionNo(family.getTemplateId(), 2).orElseThrow();

        // Assert
        assertThat(reloadedFamily.getTemplateKey()).isEqualTo("invoice.reminder");
        assertThat(reloadedV2.getStatus()).isEqualTo(TemplateStatus.APPROVED);
        assertThat(v1.getVersionNo()).isEqualTo(1);
        assertThat(v2.getVersionNo()).isEqualTo(2);
    }

    @Test
    void approveVersion_shouldSetActiveApprovedVersionInFamily() {
        // Arrange
        TemplateFamily family = fixture.createTenantFamily("tenantA", "payment.notice");
        fixture.createVersion(family.getTemplateId(), 1, TemplateStatus.DRAFT, RenderTarget.TEXT, "Draft {{name}}");
        fixture.createVersion(family.getTemplateId(), 2, TemplateStatus.DRAFT, RenderTarget.TEXT, "Draft 2 {{name}}");

        // Act
        TemplateVersionResponse approved = templateService.approveVersion(family.getTemplateId(), 2, "approver");
        TemplateFamily reloaded = familyRepository.findById(family.getTemplateId()).orElseThrow();
        TemplateVersion version1 = versionRepository.findByTemplateIdAndVersionNo(family.getTemplateId(), 1).orElseThrow();
        TemplateVersion version2 = versionRepository.findByTemplateIdAndVersionNo(family.getTemplateId(), 2).orElseThrow();

        // Assert
        assertThat(approved.versionNo()).isEqualTo(2);
        assertThat(approved.status()).isEqualTo(TemplateStatus.APPROVED);
        assertThat(reloaded.getActiveApprovedVersion()).isEqualTo(2);
        assertThat(version2.getStatus()).isEqualTo(TemplateStatus.APPROVED);
        assertThat(Objects.equals(version1.getStatus(), TemplateStatus.APPROVED)).isFalse();
    }

    @Test
    void versionUniqueConstraint_shouldRejectDuplicateVersionNoPerTemplate() {
        // Arrange
        TemplateFamily family = fixture.createTenantFamily("tenantA", "duplicate.version.check");
        fixture.createVersion(family.getTemplateId(), 1, TemplateStatus.DRAFT, RenderTarget.TEXT, "v1 {{name}}");

        // Act
        UUID versionId = UUID.randomUUID();

        // Assert
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO template_version
                (version_id, template_id, version_no, status, render_target, body_tpl, placeholders, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """,
            versionId.toString(),
            family.getTemplateId().toString(),
            1,
            "DRAFT",
            "TEXT",
            "duplicate",
            "[\"name\"]"
        )).isInstanceOf(Exception.class);
        assertThat(versionRepository.findByTemplateIdOrderByVersionNoDesc(family.getTemplateId())).hasSize(1);
    }
}
