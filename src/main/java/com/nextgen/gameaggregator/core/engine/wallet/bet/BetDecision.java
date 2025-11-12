package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.enums.BetDecisionType;
import com.nextgen.gameaggregator.core.exception.BetNotAllowedException;

public record BetDecision(BetDecisionType decisionType,
                          String reason,
                          Class<? extends Throwable> exceptionClass) {

    // Decision type checks
    public boolean isAllowed() {
        return decisionType == BetDecisionType.ALLOW;
    }

    public boolean isRejected() {
        return decisionType == BetDecisionType.REJECT;
    }

    public boolean isNoOp() {
        return decisionType == BetDecisionType.NO_OP;
    }

    // Exception handling
    public void throwIfRejected(VendorRequestContext context, BetConfig config) {
        if (isRejected()) {
            throw new BetNotAllowedException(context, createException(), config);
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

    public static BetDecision allow() {
        return new BetDecision(BetDecisionType.ALLOW, "Allow bet processing", null);
    }

    public static BetDecision reject(String reason, Class<? extends Exception> exceptionClass) {
        return new BetDecision(BetDecisionType.REJECT, reason, exceptionClass);
    }

    public static BetDecision noop() {
        return new BetDecision(BetDecisionType.NO_OP, "", null);
    }
}
