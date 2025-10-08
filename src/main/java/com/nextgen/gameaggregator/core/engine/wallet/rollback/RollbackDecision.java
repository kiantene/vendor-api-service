package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackDecisionType;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;

public record RollbackDecision(RollbackDecisionType type,
                               String reason,
                               Class<? extends Throwable> exceptionClass) {
    public boolean isAllowed()  { return type == RollbackDecisionType.ALLOW; }
    public boolean isRejected() { return type == RollbackDecisionType.REJECT; }
    public boolean isNoOp()     { return type == RollbackDecisionType.NO_OP; }

    public void throwIfRejected(VendorRequestContext context) {
        if (isRejected()) {
            throw new RollbackNotAllowedException(context, createException());
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

    public static RollbackDecision allow() {
        return new RollbackDecision(RollbackDecisionType.ALLOW, "Eligible for rollback", null);
    }

    public static RollbackDecision reject(String reason, Class<? extends Exception> exceptionClass) {
        return new RollbackDecision(RollbackDecisionType.REJECT, reason, exceptionClass);
    }

    public static RollbackDecision noop(String reason) {
        return new RollbackDecision(RollbackDecisionType.NO_OP, reason, null);
    }
}
