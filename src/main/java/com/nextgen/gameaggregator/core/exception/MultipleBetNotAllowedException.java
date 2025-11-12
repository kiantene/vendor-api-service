package com.nextgen.gameaggregator.core.exception;

public class MultipleBetNotAllowedException extends RuntimeException {
    public MultipleBetNotAllowedException() {
        super();
    }

    public MultipleBetNotAllowedException(String message) {
        super(message);
    }
}
