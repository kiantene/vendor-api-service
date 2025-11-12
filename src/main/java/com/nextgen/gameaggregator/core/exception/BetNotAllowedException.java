package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig;

public class BetNotAllowedException extends VendorCallbackException {
    public BetNotAllowedException() {
        super();
        this.config = null;
    }

    public BetNotAllowedException(VendorRequestContext context, String message, Throwable ex) {
        super(context, message, ex);
        this.config = null;
    }

    private final BetConfig config;

    public BetNotAllowedException(VendorRequestContext context, Throwable cause, BetConfig config) {
        super(context, cause);
        this.config = config;
    }

    public boolean isRoundAlreadyEnded() {
        return this.getCause() instanceof RoundAlreadyEndedException;
    }

    public boolean isMultipleBetNotAllowed() {
        return this.getCause() instanceof MultipleBetNotAllowedException;
    }

    public boolean isAllowMultipleBet() {
        if (config == null) return false;

        return config.isAllowMultipleBet();
    }
}
