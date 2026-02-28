package de.innologic.templateservice.support;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class TestSecurityContext {

    private TestSecurityContext() {
    }

    public static void setJwt(String tenantId, String subject, String... scopes) {
        List<String> validScopes = Arrays.stream(scopes == null ? new String[0] : scopes)
                .filter(scope -> scope != null && !scope.isBlank())
                .toList();
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String scope : validScopes) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }

        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue(UUID.randomUUID().toString())
                .header("alg", "none")
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .audience(List.of("template-service"))
                .claim("tenant_id", tenantId);

        if (!validScopes.isEmpty()) {
            builder.claim("scope", String.join(" ", validScopes));
        }

        Jwt jwt = builder.build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
