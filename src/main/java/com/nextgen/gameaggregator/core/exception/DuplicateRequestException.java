package com.nextgen.gameaggregator.core.exception;

public class DuplicateRequestException extends RuntimeException {
    public DuplicateRequestException() { super(); }

    public DuplicateRequestException(String message) {
        super(message);
    }

    public DuplicateRequestException(String message, Throwable ex) {
        super(message, ex);
    }
}
