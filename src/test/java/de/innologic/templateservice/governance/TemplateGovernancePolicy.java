package de.innologic.templateservice.governance;

import de.innologic.templateservice.domain.enums.TemplateScope;

import java.util.Set;

public class TemplateGovernancePolicy {

    private static final Set<String> RESERVED_GLOBAL_KEYS = Set.of(
        "system.alert",
        "system.reset-password",
        "platform.maintenance"
    );

    public void assertAllowedCreate(
        TemplateScope scope,
        String requesterTenantId,
        String ownerTenantId,
        String templateKey,
        boolean globalTemplateAlreadyExists
    ) {
        if (scope == TemplateScope.GLOBAL && requesterTenantId != null) {
            throw new IllegalStateException("GLOBAL templates require platform_admin");
        }
        if (scope == TemplateScope.TENANT && requesterTenantId != null && !requesterTenantId.equals(ownerTenantId)) {
            throw new IllegalStateException("Tenant mismatch");
        }
        if (scope == TemplateScope.TENANT && (RESERVED_GLOBAL_KEYS.contains(templateKey) || globalTemplateAlreadyExists)) {
            throw new IllegalStateException("TEMPLATE_KEY_RESERVED");
        }
    }
}
