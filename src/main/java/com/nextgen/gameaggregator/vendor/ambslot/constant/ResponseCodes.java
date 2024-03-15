package com.nextgen.gameaggregator.vendor.ambslot.constant;

public class ResponseCodes {
    // response code
    public static final Integer SUCCESS = 0;
    public static final Integer INVALID_REQUEST = 997;

    public static final Integer RESPONSE_ERROR = 1600;
    public static final Integer RESPONSE_TIMEOUT_ERROR = 1601;
    public static final Integer DUPLICATED_TRANSACTION_ERROR = 900;
    public static final Integer INSUFFICIENT_BALANCE = 800;


    // response code message
    public static final String SUCCESS_MSG = "Success";
    public static final String INVALID_REQUEST_MSG = "Invalid request data";
    public static final String INVALID_AGENT_MSG = "Invalid agent id";
    public static final String RESPONSE_ERROR_MSG = "Response error";
    public static final String RESPONSE_TIMEOUT_ERROR_MSG = "Response error timeout";
    public static final String DUPLICATED_TRANSACTION_ERROR_MSG = "Duplicate transaction id";
    public static final String INSUFFICIENT_BALANCE_MSG = "Balance insufficient";
}
