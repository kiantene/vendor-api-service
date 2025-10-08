package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

public class BetNotAllowedException extends VendorCallbackException {
    public BetNotAllowedException() {
        super();
    }

    public BetNotAllowedException(VendorRequestContext context, String message, Throwable ex) {
        super(context, message, ex);
    }
}
