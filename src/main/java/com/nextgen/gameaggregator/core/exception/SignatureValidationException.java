package com.nextgen.gameaggregator.core.exception;

public class SignatureValidationException extends RuntimeException {
    public SignatureValidationException() {
        super();
    }

    public SignatureValidationException(String message) {
        super(message);
    }

    public SignatureValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
