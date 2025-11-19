package com.nextgen.gameaggregator.core.exception;

public class RoundAlreadyVoidException extends RuntimeException {
    public RoundAlreadyVoidException() {
        super();
    }

    public RoundAlreadyVoidException(String message) {
        super(message);
    }
}
