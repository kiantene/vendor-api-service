package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

public class BetNotFoundException extends VendorCallbackException {
    public BetNotFoundException() {
        super();
    }

    public BetNotFoundException(String message) {
        super(message);
    }

    public BetNotFoundException(VendorRequestContext context, String message, Throwable ex) {
        super(context, message, ex);
    }
}
