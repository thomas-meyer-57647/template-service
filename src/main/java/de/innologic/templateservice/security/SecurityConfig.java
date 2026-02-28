package de.innologic.templateservice.security;

import tools.jackson.databind.json.JsonMapper;
import de.innologic.templateservice.api.dto.ErrorDTO;
import de.innologic.templateservice.api.error.CorrelationIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityJwtProperties.class)
public class SecurityConfig {

    private static final String API_BASE = "/template";

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JsonMapper jsonMapper,
            TenantHeaderValidationFilter tenantHeaderValidationFilter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, API_BASE + "/render").hasAuthority("SCOPE_template:read")
                        .requestMatchers(HttpMethod.POST, API_BASE + "/preview").hasAnyAuthority("SCOPE_template:read", "SCOPE_template:admin")
                        .requestMatchers(HttpMethod.POST, API_BASE + "/validate").hasAuthority("SCOPE_template:admin")
                        .requestMatchers(HttpMethod.GET, API_BASE + "/**").hasAuthority("SCOPE_template:read")
                        .requestMatchers(HttpMethod.POST, API_BASE + "/families/**").hasAnyAuthority("SCOPE_template:admin", "SCOPE_template:global:admin")
                        .requestMatchers(HttpMethod.PUT, API_BASE + "/families/**").hasAnyAuthority("SCOPE_template:admin", "SCOPE_template:global:admin")
                        .requestMatchers(HttpMethod.DELETE, API_BASE + "/families/**").hasAnyAuthority("SCOPE_template:admin", "SCOPE_template:global:admin")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .addFilterAfter(tenantHeaderValidationFilter, BearerTokenAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authEx) ->
                                writeError(
                                        request,
                                        jsonMapper,
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHENTICATED",
                                        authEx.getMessage(),
                                        request.getRequestURI()
                                ))
                        .accessDeniedHandler((request, response, deniedEx) ->
                                writeError(
                                        request,
                                        jsonMapper,
                                        response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "ACCESS_DENIED",
                                        deniedEx.getMessage(),
                                        request.getRequestURI()
                                ))
                );

        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            SecurityJwtProperties securityJwtProperties
    ) {
        NimbusJwtDecoder decoder;
        if (StringUtils.hasText(jwkSetUri)) {
            decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else if (StringUtils.hasText(securityJwtProperties.issuer())) {
            decoder = NimbusJwtDecoder.withIssuerLocation(securityJwtProperties.issuer()).build();
        } else {
            throw new IllegalStateException("Either spring.security.oauth2.resourceserver.jwt.jwk-set-uri or security.jwt.issuer must be configured");
        }
        decoder.setJwtValidator(jwtTokenValidator(securityJwtProperties));
        return decoder;
    }

    @Bean
    OAuth2TokenValidator<Jwt> jwtTokenValidator(SecurityJwtProperties securityJwtProperties) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator(Duration.ofSeconds(securityJwtProperties.clockSkewSeconds())));
        if (StringUtils.hasText(securityJwtProperties.issuer())) {
            validators.add(new JwtIssuerValidator(securityJwtProperties.issuer()));
        }
        validators.add(new AudienceValidator(securityJwtProperties.audience()));
        validators.add(new TenantIdClaimValidator());
        return new DelegatingOAuth2TokenValidator<>(validators);
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractScopeAuthorities);
        return converter;
    }

    @Bean
    TenantHeaderValidationFilter tenantHeaderValidationFilter(JsonMapper jsonMapper) {
        return new TenantHeaderValidationFilter(jsonMapper);
    }

    private Collection<GrantedAuthority> extractScopeAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        String scope = jwt.getClaimAsString("scope");
        if (StringUtils.hasText(scope)) {
            for (String value : scope.split(" ")) {
                if (StringUtils.hasText(value)) {
                    authorities.add((GrantedAuthority) () -> "SCOPE_" + value);
                }
            }
        }

        Object scpClaim = jwt.getClaims().get("scp");
        if (scpClaim instanceof Collection<?> values) {
            for (Object value : values) {
                if (value instanceof String scopeValue && StringUtils.hasText(scopeValue)) {
                    authorities.add((GrantedAuthority) () -> "SCOPE_" + scopeValue);
                }
            }
        }

        return authorities;
    }

    private void writeError(
            HttpServletRequest request,
            JsonMapper mapper,
            HttpServletResponse response,
            int status,
            String errorCode,
            String message,
            String path
    ) throws IOException {
        String correlationId = CorrelationIdResolver.resolve(request);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-Correlation-Id", correlationId);
        mapper.writeValue(response.getWriter(), new ErrorDTO(
                Instant.now(),
                status,
                errorCode,
                message,
                Map.of(),
                path,
                correlationId
        ));
    }
}
