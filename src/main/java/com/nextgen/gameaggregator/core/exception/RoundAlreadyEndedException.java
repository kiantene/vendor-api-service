package com.nextgen.gameaggregator.core.exception;

public class RoundAlreadyEndedException extends RuntimeException {
    public RoundAlreadyEndedException() {
        super();
    }

    public RoundAlreadyEndedException(String message) {
        super(message);
    }
}
