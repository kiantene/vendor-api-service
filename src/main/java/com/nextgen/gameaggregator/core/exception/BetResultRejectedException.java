package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;

public class BetResultRejectedException extends VendorCallbackException {
    private final BetResultConfig config;

    public BetResultRejectedException(VendorRequestContext context, Throwable cause, BetResultConfig config) {
        super(context, cause);
        this.config = config;
    }

    public boolean isRoundAlreadyEnded() {
        return this.getCause() instanceof RoundAlreadyEndedException;
    }

    public boolean isRoundNotFound() {
        return this.getCause() instanceof RoundNotFoundException;
    }

    public boolean isBetAndResult() {
        if (config == null) return false;

        return config.isBetAndResult();
    }
}
