package com.nextgen.gameaggregator.core.exception;

public class BetNotFoundException extends RuntimeException {
    public BetNotFoundException() {
        super();
    }

    public BetNotFoundException(String message, Throwable ex) {
        super(message, ex);
    }
}
