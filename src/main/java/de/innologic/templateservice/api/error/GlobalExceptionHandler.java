package de.innologic.templateservice.api.error;

import de.innologic.templateservice.api.dto.ErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorDTO> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.errorCode(), ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorDTO> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, resolveConflictCode(ex.getMessage()), ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(UnprocessableTemplateException.class)
    public ResponseEntity<ErrorDTO> handleUnprocessable(UnprocessableTemplateException ex, HttpServletRequest request) {
        String errorCode;
        if (ex.getMessage() != null && ex.getMessage().contains("MISSING_KEYS")) {
            errorCode = "MISSING_KEYS";
        } else if ("TEMPLATE_KEY_RESERVED".equals(ex.getMessage())) {
            errorCode = "TEMPLATE_KEY_RESERVED";
        } else {
            errorCode = "UNPROCESSABLE_TEMPLATE";
        }
        Map<String, Object> details = ex.getMissingKeys().isEmpty()
                ? Map.of()
                : Map.of("missingKeys", ex.getMissingKeys());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, errorCode, ex.getMessage(), details, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::formatFieldError)
            .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, Map.of(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDTO> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDTO> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMostSpecificCause().getMessage(), Map.of(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDTO> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDTO> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), Map.of(), request);
    }

    private static String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private String resolveConflictCode(String message) {
        if (message != null && message.matches("^[A-Z0-9_]+$")) {
            return message;
        }
        return "CONFLICT";
    }

    private ResponseEntity<ErrorDTO> build(
            HttpStatus status,
            String errorCode,
            String message,
            Map<String, Object> details,
            HttpServletRequest request
    ) {
        String correlationId = resolveCorrelationId(request);
        return ResponseEntity.status(status)
                .header("X-Correlation-Id", correlationId)
                .body(
            new ErrorDTO(
                    Instant.now(),
                    status.value(),
                    errorCode,
                    message,
                    details,
                    request.getRequestURI(),
                    correlationId
            )
        );
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        return CorrelationIdResolver.resolve(request);
    }
}
