package com.nextgen.gameaggregator.core.exception;

public class BetNotAllowedException extends RuntimeException {
    public BetNotAllowedException() {
        super();
    }

    public BetNotAllowedException(String message, Throwable ex) {
        super(message, ex);
    }
}
