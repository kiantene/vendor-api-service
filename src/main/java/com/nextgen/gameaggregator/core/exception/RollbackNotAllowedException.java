package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

public class RollbackNotAllowedException extends VendorCallbackException {

    public RollbackNotAllowedException(String message) {
        super(message);
    }

    public RollbackNotAllowedException(VendorRequestContext context, Throwable ex) {
        super(context, ex);
    }
}
