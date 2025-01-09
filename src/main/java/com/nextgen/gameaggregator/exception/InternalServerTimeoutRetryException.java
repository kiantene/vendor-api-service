package com.nextgen.gameaggregator.exception;

public class InternalServerTimeoutRetryException extends Exception {
    public InternalServerTimeoutRetryException() {
        super();
    }

    public InternalServerTimeoutRetryException(String message) {
        super(message);
    }
}
