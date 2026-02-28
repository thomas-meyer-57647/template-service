package de.innologic.templateservice.security;

import tools.jackson.databind.json.JsonMapper;
import de.innologic.templateservice.api.dto.ErrorDTO;
import de.innologic.templateservice.api.error.CorrelationIdResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class TenantHeaderValidationFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String ERROR_CODE = "TENANT_MISMATCH";
    private final JsonMapper jsonMapper;

    public TenantHeaderValidationFilter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String headerTenantId = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(headerTenantId)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtToken) {
                Jwt jwt = jwtToken.getToken();
                String claimTenantId = jwt.getClaimAsString("tenant_id");
                if (StringUtils.hasText(claimTenantId) && !headerTenantId.equals(claimTenantId)) {
                    writeTenantMismatchError(request, response, headerTenantId, claimTenantId);
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeTenantMismatchError(
            HttpServletRequest request,
            HttpServletResponse response,
            String headerTenantId,
            String claimTenantId
    ) throws IOException {
        String correlationId = CorrelationIdResolver.resolve(request);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setHeader("X-Correlation-Id", correlationId);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorDTO error = new ErrorDTO(
                Instant.now(),
                HttpServletResponse.SC_FORBIDDEN,
                ERROR_CODE,
                String.format("Tenant header '%s' does not match authenticated tenant '%s'.", headerTenantId, claimTenantId),
                Map.of("tenantIdHeader", headerTenantId, "tenantIdClaim", claimTenantId),
                request.getRequestURI(),
                correlationId
        );
        jsonMapper.writeValue(response.getWriter(), error);
    }
}
