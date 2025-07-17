package com.nextgen.gameaggregator.core.exception;

public class VendorNetworkException extends VendorApiException {
    public VendorNetworkException(String message, String url, Throwable cause) {
        super(message, url, cause);
    }
}
