package com.nextgen.gameaggregator.exception;

import com.nextgen.gameaggregator.entity.RawBetResultLog;
import com.nextgen.gameaggregator.entity.SettledBet;

public class SettledBetIdempotentViolationException extends Exception {
    private SettledBet settledBet;

    public SettledBetIdempotentViolationException() {
        super();
    }

    public SettledBetIdempotentViolationException(String message) {
        super(message);
    }

    public void setSettledBet(SettledBet settledBet) {
        this.settledBet = settledBet;
    }

    public SettledBet getSettledBet() {
        return this.settledBet;
    }
}
