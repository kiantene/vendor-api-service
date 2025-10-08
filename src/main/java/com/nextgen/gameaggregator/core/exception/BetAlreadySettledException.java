package com.nextgen.gameaggregator.core.exception;

public class BetAlreadySettledException extends RuntimeException {
    public BetAlreadySettledException() {
        super();
    }

    public BetAlreadySettledException(String message) {
        super(message);
    }
}
