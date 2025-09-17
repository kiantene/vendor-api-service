package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

public class GameTerminatedException extends VendorCallbackException {
    public GameTerminatedException() { super(); }

    public GameTerminatedException(VendorRequestContext context, String message) {
        super(context, message);
    }
}
