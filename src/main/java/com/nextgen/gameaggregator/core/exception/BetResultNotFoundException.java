package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

public class BetResultNotFoundException extends VendorCallbackException {
    public BetResultNotFoundException() {
        super();
    }

    public BetResultNotFoundException(String message) {
        super(message);
    }

    public BetResultNotFoundException(VendorRequestContext context, String message, Throwable ex) {
        super(context, message, ex);
    }
}
