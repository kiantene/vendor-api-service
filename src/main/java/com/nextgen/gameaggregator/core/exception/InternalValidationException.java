package com.nextgen.gameaggregator.core.exception;

public class InternalValidationException extends RuntimeException {
    public InternalValidationException() {
        super();
    }

    public InternalValidationException(String message) {
        super(message);
    }
}
