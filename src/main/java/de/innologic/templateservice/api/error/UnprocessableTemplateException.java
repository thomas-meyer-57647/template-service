package de.innologic.templateservice.api.error;

import java.util.List;

public class UnprocessableTemplateException extends RuntimeException {
    private final List<String> missingKeys;

    public UnprocessableTemplateException(String message) {
        super(message);
        this.missingKeys = List.of();
    }

    public UnprocessableTemplateException(String message, List<String> missingKeys) {
        super(message);
        this.missingKeys = missingKeys == null ? List.of() : List.copyOf(missingKeys);
    }

    public List<String> getMissingKeys() {
        return missingKeys;
    }
}
