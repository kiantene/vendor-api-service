package com.nextgen.gameaggregator.core.exception;

public class RollbackNotAllowedException extends RuntimeException {
    public RollbackNotAllowedException() {
        super();
    }

    public RollbackNotAllowedException(String message) {
        super(message);
    }

    public RollbackNotAllowedException(String message, Throwable ex) {
        super(message, ex);
    }
}
