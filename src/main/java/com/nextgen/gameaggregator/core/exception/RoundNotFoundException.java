package com.nextgen.gameaggregator.core.exception;

public class RoundNotFoundException extends RuntimeException {
    public RoundNotFoundException() {
        super();
    }

    public RoundNotFoundException(String message) {
        super(message);
    }
}
