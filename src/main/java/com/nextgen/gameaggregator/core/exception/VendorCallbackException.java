package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import lombok.Getter;

@Getter
public abstract class VendorCallbackException extends RuntimeException {
    private final VendorRequestContext context;

    protected  VendorCallbackException() {
        super();
        this.context = null;
    }

    protected VendorCallbackException(String message) {
        super(message);
        this.context = null;
    }

    protected VendorCallbackException(VendorRequestContext context) {
        super();
        this.context = context;
    }

    protected VendorCallbackException(VendorRequestContext context, String message) {
        super(message);
        this.context = context;
    }

    protected VendorCallbackException(VendorRequestContext context, String message, Throwable ex) {
        super(message, ex);
        this.context = context;
    }

    public String getTransactionId() {
        if (context != null) {
            return context.getTransactionId();
        }
        return null;
    }
}
