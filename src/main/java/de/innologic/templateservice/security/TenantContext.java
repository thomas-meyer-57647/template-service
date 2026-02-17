package de.innologic.templateservice.security;

public record TenantContext(
        String tenantId,
        String actor
) {
}

