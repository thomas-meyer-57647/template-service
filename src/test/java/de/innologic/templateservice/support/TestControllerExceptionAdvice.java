package de.innologic.templateservice.support;

import de.innologic.templateservice.api.error.ConflictException;
import de.innologic.templateservice.api.error.NotFoundException;
import de.innologic.templateservice.api.error.UnprocessableTemplateException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class TestControllerExceptionAdvice {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<TestErrorDto> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), Collections.emptyMap(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<TestErrorDto> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), Collections.emptyMap(), request.getRequestURI());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<TestErrorDto> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "TEMPLATE_KEY_RESERVED", ex.getMessage(), Collections.emptyMap(), request.getRequestURI());
    }

    @ExceptionHandler(UnprocessableTemplateException.class)
    public ResponseEntity<TestErrorDto> handleUnprocessable(UnprocessableTemplateException ex, HttpServletRequest request) {
        Map<String, Object> details = ex.getMessage().contains("missingKeys=")
            ? Map.of("missingKeys", ex.getMessage().replace("missingKeys=", ""))
            : Collections.emptyMap();
        String errorCode = ex.getMessage().contains("TEMPLATE_SYNTAX_ERROR")
            ? "TEMPLATE_SYNTAX_ERROR"
            : ex.getMessage().contains("VERSION_NOT_RENDERABLE")
                ? "VERSION_NOT_RENDERABLE"
                : ex.getMessage().contains("MISSING_KEYS")
                    ? "MISSING_KEYS"
                    : "UNPROCESSABLE_TEMPLATE";
        return build(HttpStatus.UNPROCESSABLE_ENTITY, errorCode, ex.getMessage(), details, request.getRequestURI());
    }

    private ResponseEntity<TestErrorDto> build(
        HttpStatus status,
        String errorCode,
        String message,
        Map<String, Object> details,
        String path
    ) {
        return ResponseEntity.status(status).body(
            new TestErrorDto(Instant.now(), status.value(), errorCode, message, details, path)
        );
    }
}
