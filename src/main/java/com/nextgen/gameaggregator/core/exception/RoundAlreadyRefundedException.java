package com.nextgen.gameaggregator.core.exception;

public class RoundAlreadyRefundedException extends RuntimeException {
    public RoundAlreadyRefundedException() {
        super();
    }

    public RoundAlreadyRefundedException(String message) {
        super(message);
    }
}
