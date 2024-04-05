package com.nextgen.gameaggregator.exception;

import com.nextgen.gameaggregator.entity.ga.RawBetAdjustmentLog;
import lombok.Data;

@Data
public class BetAdjustmentIdempotentViolationException extends Exception {

    private RawBetAdjustmentLog rawBetAdjustmentLog;

    public BetAdjustmentIdempotentViolationException() {
        super();
    }

    public BetAdjustmentIdempotentViolationException(String message) {
        super(message);
    }

    public BetAdjustmentIdempotentViolationException(RawBetAdjustmentLog rawBetAdjustmentLog) {
        this.rawBetAdjustmentLog = rawBetAdjustmentLog;
    }
}
