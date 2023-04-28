package com.nextgen.gameaggregator.exception;

import com.nextgen.gameaggregator.entity.RawBetResultLog;

public class BetResultIdempotentViolationException extends Exception {
    private RawBetResultLog betResultLog;

    public BetResultIdempotentViolationException() {
        super();
    }

    public BetResultIdempotentViolationException(String message) {
        super(message);
    }

    public void setBetResultLog(RawBetResultLog betResultLog) {
        this.betResultLog = betResultLog;
    }

    public RawBetResultLog getBetResultLog() {
        return this.betResultLog;
    }
}
