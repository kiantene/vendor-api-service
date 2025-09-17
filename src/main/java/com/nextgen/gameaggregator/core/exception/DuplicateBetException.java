package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

public class DuplicateBetException extends VendorCallbackException {
    public DuplicateBetException() { super(); }

    public DuplicateBetException(VendorRequestContext context, String message) {
        super(context, message);
    }
}
