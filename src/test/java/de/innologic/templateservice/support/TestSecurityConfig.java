package de.innologic.templateservice.support;

import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.test.context.TestConfiguration;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@TestConfiguration
public class TestSecurityConfig {

    // Wenn dein Controller @RequestMapping("/template") nutzt, setz das hier auf "/template".
    // Wenn du Versionierung nutzt, ist "/api/v1/template" üblich.
    private static final String API_BASE = "/api/v1/template";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,   API_BASE + "/families/**").hasAnyAuthority("ROLE_tenant_admin", "ROLE_platform_admin")
                        .requestMatchers(HttpMethod.PUT,    API_BASE + "/families/**").hasAnyAuthority("ROLE_tenant_admin", "ROLE_platform_admin")
                        .requestMatchers(HttpMethod.DELETE, API_BASE + "/families/**").hasAnyAuthority("ROLE_tenant_admin", "ROLE_platform_admin")
                        .requestMatchers(HttpMethod.GET,    API_BASE + "/families/**").authenticated()
                        .requestMatchers(API_BASE + "/render", API_BASE + "/preview", API_BASE + "/validate").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authEx) ->
                                writeError(jsonMapper, response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHENTICATED",
                                        "JWT is required", request.getRequestURI(), Collections.emptyMap()))
                        .accessDeniedHandler((request, response, deniedEx) ->
                                writeError(jsonMapper, response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                                        deniedEx.getMessage(), request.getRequestURI(), Collections.emptyMap()))
                );

        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("tenant_id", "tenant-test")
                .build();
    }

    private static void writeError(
            JsonMapper mapper,
            HttpServletResponse response,
            int status,
            String errorCode,
            String message,
            String path,
            Map<String, Object> details
    ) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getWriter(), new TestErrorDto(
                Instant.now(),
                status,
                errorCode,
                message,
                details,
                path
        ));
    }
}
