package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.wallet.adjustment.enums.AdjustmentDecisionType;
import com.nextgen.gameaggregator.core.exception.BetResultRejectedException;

public record AdjustmentDecision(AdjustmentDecisionType type,
                                 String reason,
                                 Class<? extends Throwable> exceptionClass) {

    public boolean isAllowed()  { return type == AdjustmentDecisionType.ALLOW; }
    public boolean isRejected() { return type == AdjustmentDecisionType.REJECT; }
    public boolean isNoOp()     { return type == AdjustmentDecisionType.NO_OP; }

    public void throwIfRejected(VendorRequestContext context, AdjustmentConfig config) {
        if (isRejected()) {
            throw new BetResultRejectedException(context, createException(), null);
        }
    }

    public RuntimeException createException() {
        if (exceptionClass == null) {
            return new RuntimeException(reason);
        }

        try {
            return (RuntimeException) exceptionClass
                    .getDeclaredConstructor(String.class)
                    .newInstance(reason);
        } catch (Exception e) {
            return new RuntimeException("Failed to create exception: " + reason, e);
        }
    }

    public static AdjustmentDecision allow() {
        return new AdjustmentDecision(AdjustmentDecisionType.ALLOW, "Allow adjustment processing", null);
    }

    public static AdjustmentDecision reject(String reason, Class<? extends Exception> exceptionClass) {
        return new AdjustmentDecision(AdjustmentDecisionType.REJECT, reason, exceptionClass);
    }

    public static AdjustmentDecision noop() {
        return new AdjustmentDecision(AdjustmentDecisionType.NO_OP, "", null);
    }
}
