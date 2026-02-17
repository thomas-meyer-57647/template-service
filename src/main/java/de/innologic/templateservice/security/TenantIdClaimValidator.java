package de.innologic.templateservice.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

public class TenantIdClaimValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_TENANT =
            new OAuth2Error("invalid_token", "Missing required claim: tenant_id", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String tenantId = token.getClaimAsString("tenant_id");
        if (StringUtils.hasText(tenantId)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INVALID_TENANT);
    }
}

