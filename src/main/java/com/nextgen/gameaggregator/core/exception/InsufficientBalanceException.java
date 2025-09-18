package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

public class InsufficientBalanceException extends VendorCallbackException {
    public InsufficientBalanceException() {
        super();
    }

    public InsufficientBalanceException(VendorRequestContext context) {
        super(context);
    }

    public InsufficientBalanceException(VendorRequestContext context, String message) {
        super(context, message);
    }
}
