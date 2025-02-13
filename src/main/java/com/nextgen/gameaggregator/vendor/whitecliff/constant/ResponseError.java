package com.nextgen.gameaggregator.vendor.whitecliff.constant;

public class ResponseError {
    public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String DUPLICATE_DEBIT = "DUPLICATE_DEBIT";
    public static final String DUPLICATE_BONUS = "DUPLICATE_BONUS";
    public static final String INVALID_DEBIT = "INVALID_DEBIT";
    public static final String DUPLICATE_CREDIT = "DUPLICATE_CREDIT";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String INVALID_USER = "INVALID_USER";



    private ResponseError() {
    }

    public static ResponseError createResponseError() {
        return new ResponseError();
    }
}