package de.innologic.templateservice.support;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class TestJwtFactory {

    private TestJwtFactory() {
    }

    public static RequestPostProcessor jwtWithTenantAndRoles(String tenantId, String... roles) {
        List<org.springframework.security.core.GrantedAuthority> authorities = Arrays.stream(roles)
            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
            .map(role -> (org.springframework.security.core.GrantedAuthority) new SimpleGrantedAuthority(role))
            .toList();

        return jwt().jwt(builder -> builder.claim("tenant_id", tenantId)).authorities(authorities);
    }
}
