package com.nextgen.gameaggregator.exception;

import com.nextgen.gameaggregator.entity.RawBetResultLog;

public class SettledBetIdempotentViolationException extends Exception {
    private RawBetResultLog betResultLog;

    public SettledBetIdempotentViolationException() {
        super();
    }

    public SettledBetIdempotentViolationException(String message) {
        super(message);
    }
}
