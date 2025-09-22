package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.BetResultDecisionType;
import com.nextgen.gameaggregator.core.exception.BetResultRejectedException;

public record BetResultDecision (BetResultDecisionType type,
                                 String reason,
                                 Class<? extends Throwable> exceptionClass) {

    public boolean isAllowed()  { return type == BetResultDecisionType.ALLOW; }
    public boolean isRejected() { return type == BetResultDecisionType.REJECT; }
    public boolean isNoOp()     { return type == BetResultDecisionType.NO_OP; }

    public void throwIfRejected(VendorRequestContext context) {
        if (isRejected()) {
            throw new BetResultRejectedException(context, createException());
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

    public static BetResultDecision allow() {
        return new BetResultDecision(BetResultDecisionType.ALLOW, "Allow settlement processing", null);
    }

    public static BetResultDecision reject(String reason, Class<? extends Exception> exceptionClass) {
        return new BetResultDecision(BetResultDecisionType.REJECT, reason, exceptionClass);
    }
}
