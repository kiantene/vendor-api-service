package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

public class GameSessionExpiredException extends VendorCallbackException {

    public GameSessionExpiredException() { super(); }

    public GameSessionExpiredException(VendorRequestContext context, String message) {
        super(context, message);
    }
}
