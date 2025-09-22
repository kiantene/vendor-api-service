package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

public class BetResultRejectedException extends VendorCallbackException {
    public BetResultRejectedException() {
        super();
    }

    public BetResultRejectedException(VendorRequestContext context, Throwable cause) {
        super(context, cause);
    }

    public boolean isRoundAlreadyEnded() {
        return this.getCause() instanceof RoundAlreadyEndedException;
    }

    public boolean isRoundNotFound() {
        return this.getCause() instanceof RoundNotFoundException;
    }
}
