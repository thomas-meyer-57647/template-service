package de.innologic.templateservice.security;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TenantContextResolver {

    public TenantContext resolveRequired() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new InsufficientAuthenticationException("JWT authentication is required");
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (!StringUtils.hasText(tenantId)) {
            throw new InsufficientAuthenticationException("Missing required claim: tenant_id");
        }

        String actor = jwt.getSubject();
        if (!StringUtils.hasText(actor)) {
            throw new InsufficientAuthenticationException("Missing required claim: sub");
        }

        return new TenantContext(tenantId, actor);
    }
}

