package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

public class RollbackNotAllowedException extends VendorCallbackException {

    public RollbackNotAllowedException(String message) {
        super(message);
    }

    public RollbackNotAllowedException(VendorRequestContext context, Throwable ex) {
        super(context, ex);
    }

    public boolean isBetNotFound() {
        return this.getCause() instanceof BetNotFoundException;
    }

    public boolean isBetAlreadySettled() {
        return this.getCause() instanceof BetAlreadySettledException;
    }

    public boolean isRoundAlreadyEnded() {
        return this.getCause() instanceof RoundAlreadyEndedException;
    }
}
