package de.innologic.templateservice.support;

import java.time.Instant;
import java.util.Map;

public record TestErrorDto(
    Instant timestamp,
    int status,
    String errorCode,
    String message,
    Map<String, Object> details,
    String path
) {
}
