package com.nextgen.gameaggregator.vendor.whitecliff.constant;

public class ResponseCodes {
    public static final int SUCCESS = 1;
    public static final int FAILED = 0;

    private ResponseCodes() {
    }

    public static ResponseCodes createResponseCodes() {
        return new ResponseCodes();
    }
}
