package com.nextgen.gameaggregator.exception;

public class VendorApiException extends Exception {
    private String errorCode;

    public VendorApiException() {
        super();
    }

    public VendorApiException(String message) {
        super(message);
    }
    public VendorApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return this.errorCode;
    }
}
