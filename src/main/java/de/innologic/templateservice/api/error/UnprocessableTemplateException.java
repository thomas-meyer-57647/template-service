package de.innologic.templateservice.api.error;

public class UnprocessableTemplateException extends RuntimeException {
    public UnprocessableTemplateException(String message) {
        super(message);
    }
}
