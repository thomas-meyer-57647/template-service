package de.innologic.templateservice.api.error;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class CorrelationIdResolver {

    private CorrelationIdResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (hasText(requestId)) {
            return requestId;
        }
        String correlationId = request.getHeader("X-Correlation-Id");
        if (hasText(correlationId)) {
            return correlationId;
        }
        String traceparent = request.getHeader("traceparent");
        if (hasText(traceparent)) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 4 && hasText(parts[1])) {
                return parts[1];
            }
        }
        return UUID.randomUUID().toString();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
