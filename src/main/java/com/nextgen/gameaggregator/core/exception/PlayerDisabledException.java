package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

public class PlayerDisabledException extends VendorCallbackException {
    public PlayerDisabledException(VendorRequestContext context, String message) {
        super(context, message);
    }
}
