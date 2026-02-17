package de.innologic.templateservice.persistence;

import de.innologic.templateservice.api.dto.TemplateVersionResponse;
import de.innologic.templateservice.api.dto.TemplateVersionRequest;
import de.innologic.templateservice.api.dto.TemplateFamilyRequest;
import de.innologic.templateservice.api.dto.PageResponse;
import de.innologic.templateservice.api.dto.CatalogTemplateVersionResponse;
import de.innologic.templateservice.api.error.ConflictException;
import de.innologic.templateservice.api.error.UnprocessableTemplateException;
import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.entity.TemplateVersion;
import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import de.innologic.templateservice.domain.repository.TemplateFamilyRepository;
import de.innologic.templateservice.domain.repository.TemplateVersionRepository;
import de.innologic.templateservice.events.TemplateDomainEvent;
import de.innologic.templateservice.events.TemplateEventPublisher;
import de.innologic.templateservice.events.TemplateVersionApprovedEvent;
import de.innologic.templateservice.events.TemplateVersionCreatedEvent;
import de.innologic.templateservice.events.TemplateVersionDeprecatedEvent;
import de.innologic.templateservice.service.TemplateService;
import de.innologic.templateservice.support.AbstractMariaDbIntegrationTest;
import de.innologic.templateservice.support.TemplateTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

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

    @MockitoBean
    private TemplateEventPublisher templateEventPublisher;

    @BeforeEach
    void cleanDatabase() {
        versionRepository.deleteAll();
        familyRepository.deleteAll();
        reset(templateEventPublisher);
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
        fixture.createVersion(family.getTemplateId(), 1, TemplateStatus.APPROVED, RenderTarget.TEXT, "Old approved {{name}}");
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
        assertThat(version1.getStatus()).isEqualTo(TemplateStatus.DEPRECATED);
    }

    @Test
    void updateExistingVersion_shouldFailBecauseImmutable() {
        // Arrange
        TemplateFamily family = fixture.createTenantFamily("tenantA", "immutable.version");
        fixture.createVersion(family.getTemplateId(), 1, TemplateStatus.DRAFT, RenderTarget.TEXT, "v1 {{name}}");
        TemplateVersionRequest updateRequest = new TemplateVersionRequest(
            99,
            TemplateStatus.APPROVED,
            RenderTarget.HTML,
            "changed",
            "<p>changed</p>",
            "[\"name\"]",
            "editor"
        );

        // Act + Assert
        assertThatThrownBy(() -> templateService.updateVersion(family.getTemplateId(), 1, updateRequest))
            .isInstanceOf(ConflictException.class)
            .hasMessage("VERSION_IMMUTABLE");
    }

    @Test
    void createVersion_shouldAlwaysIncreaseVersionNoAndPersistAsDraft() {
        // Arrange
        TemplateFamily family = fixture.createTenantFamily("tenantA", "create.version.increment");
        fixture.createVersion(family.getTemplateId(), 1, TemplateStatus.APPROVED, RenderTarget.TEXT, "v1 {{name}}");

        TemplateVersionRequest createRequest = new TemplateVersionRequest(
            777,
            TemplateStatus.APPROVED,
            RenderTarget.TEXT,
            "subject {{name}}",
            "v2 {{name}}",
            "[\"name\"]",
            "author"
        );

        // Act
        TemplateVersionResponse created = templateService.createVersion(family.getTemplateId(), createRequest);

        // Assert
        assertThat(created.versionNo()).isEqualTo(2);
        assertThat(created.status()).isEqualTo(TemplateStatus.DRAFT);
    }

    @Test
    void createVersion_shouldExtractAndPersistPlaceholders() {
        // Arrange
        TemplateFamily family = fixture.createTenantFamily("tenantA", "placeholder.extract");
        TemplateVersionRequest createRequest = new TemplateVersionRequest(
            null,
            TemplateStatus.DRAFT,
            RenderTarget.HTML,
            "Hello {{firstName}}",
            "<p>{{firstName}} {{order.id}}</p>",
            "[]",
            "author"
        );

        // Act
        TemplateVersionResponse created = templateService.createVersion(family.getTemplateId(), createRequest);
        TemplateVersion saved = versionRepository.findByTemplateIdAndVersionNo(family.getTemplateId(), created.versionNo()).orElseThrow();

        // Assert
        assertThat(saved.getPlaceholders()).isEqualTo("[\"firstName\",\"order.id\"]");
    }

    @Test
    void tenantReservedKey_shouldBeRejected() {
        TemplateFamilyRequest request = new TemplateFamilyRequest(
            TemplateScope.TENANT,
            "tenantA",
            "system.reset-password",
            "EMAIL",
            "de-DE",
            "SYSTEM",
            "tester"
        );

        assertThatThrownBy(() -> templateService.createFamily(request))
            .isInstanceOf(UnprocessableTemplateException.class)
            .hasMessage("TEMPLATE_KEY_RESERVED");
    }

    @Test
    void tenantShadowingGlobal_shouldBeRejected() {
        TemplateFamily global = new TemplateFamily();
        global.setTemplateId(UUID.randomUUID());
        global.setScope(TemplateScope.GLOBAL);
        global.setOwnerTenantId("__GLOBAL__");
        global.setTemplateKey("email.confirmation");
        global.setChannel("EMAIL");
        global.setLocale("de-DE");
        global.setCategory("SYSTEM");
        global.setCreatedBy("platform");
        global.setUpdatedBy("platform");
        familyRepository.save(global);

        TemplateFamilyRequest tenantRequest = new TemplateFamilyRequest(
            TemplateScope.TENANT,
            "tenantA",
            "email.confirmation",
            "EMAIL",
            "de-DE",
            "BILLING",
            "tester"
        );

        assertThatThrownBy(() -> templateService.createFamily(tenantRequest))
            .isInstanceOf(ConflictException.class)
            .hasMessage("TEMPLATE_SHADOWING_FORBIDDEN");
    }

    @Test
    void catalogApprovedVersions_shouldReturnOnlyApproved() {
        TemplateFamily family = fixture.createTenantFamily("tenantA", "catalog.approved.only");
        fixture.createVersion(family.getTemplateId(), 1, TemplateStatus.DRAFT, RenderTarget.TEXT, "v1");
        fixture.createVersion(family.getTemplateId(), 2, TemplateStatus.APPROVED, RenderTarget.TEXT, "v2");
        fixture.createVersion(family.getTemplateId(), 3, TemplateStatus.DEPRECATED, RenderTarget.TEXT, "v3");

        PageResponse<CatalogTemplateVersionResponse> result = templateService.catalogApprovedVersions(
                family.getTemplateId(),
                0,
                50,
                "versionNo,DESC"
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).versionNo()).isEqualTo(2);
        assertThat(result.items().get(0).status()).isEqualTo(TemplateStatus.APPROVED);
    }

    @Test
    void createVersion_shouldPublishTemplateVersionCreatedEvent() {
        TemplateFamily family = fixture.createTenantFamily("tenantA", "event.version.created");
        TemplateVersionRequest createRequest = new TemplateVersionRequest(
                null,
                TemplateStatus.DRAFT,
                RenderTarget.TEXT,
                "subject {{name}}",
                "body {{name}}",
                "[\"name\"]",
                "creator-sub"
        );

        templateService.createVersion(family.getTemplateId(), createRequest);

        ArgumentCaptor<TemplateDomainEvent> captor = ArgumentCaptor.forClass(TemplateDomainEvent.class);
        verify(templateEventPublisher, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues())
                .anySatisfy(event -> {
                    assertThat(event).isInstanceOf(TemplateVersionCreatedEvent.class);
                    assertThat(event.templateId()).isEqualTo(family.getTemplateId());
                    assertThat(event.versionNo()).isEqualTo(1);
                    assertThat(event.actorSub()).isEqualTo("creator-sub");
                });
    }

    @Test
    void approveVersion_shouldPublishApprovedAndDeprecatedEvents() {
        TemplateFamily family = fixture.createTenantFamily("tenantA", "event.version.approve");
        fixture.createVersion(family.getTemplateId(), 1, TemplateStatus.APPROVED, RenderTarget.TEXT, "v1");
        fixture.createVersion(family.getTemplateId(), 2, TemplateStatus.DRAFT, RenderTarget.TEXT, "v2");

        templateService.approveVersion(family.getTemplateId(), 2, "approver-sub");

        ArgumentCaptor<TemplateDomainEvent> captor = ArgumentCaptor.forClass(TemplateDomainEvent.class);
        verify(templateEventPublisher, atLeastOnce()).publish(captor.capture());
        List<TemplateDomainEvent> events = captor.getAllValues();

        assertThat(events)
                .anySatisfy(event -> {
                    assertThat(event).isInstanceOf(TemplateVersionApprovedEvent.class);
                    assertThat(event.templateId()).isEqualTo(family.getTemplateId());
                    assertThat(event.versionNo()).isEqualTo(2);
                    assertThat(event.actorSub()).isEqualTo("approver-sub");
                })
                .anySatisfy(event -> {
                    assertThat(event).isInstanceOf(TemplateVersionDeprecatedEvent.class);
                    assertThat(event.templateId()).isEqualTo(family.getTemplateId());
                    assertThat(event.versionNo()).isEqualTo(1);
                    assertThat(event.actorSub()).isEqualTo("approver-sub");
                });
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
