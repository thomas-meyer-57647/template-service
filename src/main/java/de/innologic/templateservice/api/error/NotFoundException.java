package de.innologic.templateservice.api.error;

public class NotFoundException extends RuntimeException {

    private final String errorCode;

    public NotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
