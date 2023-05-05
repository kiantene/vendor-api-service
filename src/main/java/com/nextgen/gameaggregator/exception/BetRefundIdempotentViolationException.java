package com.nextgen.gameaggregator.exception;

import com.nextgen.gameaggregator.entity.RawBetRefundLog;

public class BetRefundIdempotentViolationException extends Exception {
    private RawBetRefundLog betRefundLog;

    public BetRefundIdempotentViolationException() {
        super();
    }

    public BetRefundIdempotentViolationException(String message) {
        super(message);
    }

    public void setBetRefundLog(RawBetRefundLog betRefundLog) {
        this.betRefundLog = betRefundLog;
    }

    public RawBetRefundLog getBetRefundLog() {
        return this.betRefundLog;
    }
}
