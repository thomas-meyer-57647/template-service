package de.innologic.templateservice.governance;

import de.innologic.templateservice.domain.enums.TemplateScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateGovernancePolicyTest {

    private final TemplateGovernancePolicy policy = new TemplateGovernancePolicy();

    @Test
    void tenantAdmin_creatingGlobalTemplate_shouldBeRejected() {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> policy.assertAllowedCreate(
            TemplateScope.GLOBAL,
            "tenantA",
            null,
            "global.any",
            false
        )).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("platform_admin");
    }

    @Test
    void tenantNoShadowing_globalReservedKey_shouldBeRejected() {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> policy.assertAllowedCreate(
            TemplateScope.TENANT,
            "tenantA",
            "tenantA",
            "system.reset-password",
            false
        )).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("TEMPLATE_KEY_RESERVED");
    }

    @Test
    void tenantNoShadowing_existingGlobalKey_shouldBeRejected() {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> policy.assertAllowedCreate(
            TemplateScope.TENANT,
            "tenantA",
            "tenantA",
            "invoice.reminder",
            true
        )).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("TEMPLATE_KEY_RESERVED");
    }

    @Test
    void tenantCanCreateOwnTenantTemplate_whenNoConflict() {
        // Arrange + Act + Assert
        assertThatCode(() -> policy.assertAllowedCreate(
            TemplateScope.TENANT,
            "tenantA",
            "tenantA",
            "invoice.reminder",
            false
        )).doesNotThrowAnyException();
    }
}
