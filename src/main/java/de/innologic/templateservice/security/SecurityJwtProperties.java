package de.innologic.templateservice.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record SecurityJwtProperties(
        String issuer,
        String audience,
        long clockSkewSeconds
) {
    public SecurityJwtProperties {
        if (audience == null || audience.isBlank()) {
            audience = "template-service";
        }
        if (clockSkewSeconds <= 0) {
            clockSkewSeconds = 60;
        }
    }
}

