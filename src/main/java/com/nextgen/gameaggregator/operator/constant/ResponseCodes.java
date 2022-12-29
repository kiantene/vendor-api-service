package com.nextgen.gameaggregator.operator.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {
    public static final String SUCCESS = "SC_OK";
    public static final String INVALID_REQUEST = "SC_INVALID_REQUEST";
    public static final String AUTHENTICATION_FAILED = "SC_AUTHENTICATION_FAILED";
    public static final String INVALID_SIGNATURE = "SC_INVALID_SIGNATURE";
    public static final String INVALID_GAME = "SC_INVALID_GAME";
    public static final String INVALID_CURRENCY = "SC_INVALID_CURRENCY";
    public static final String DUPLICATE_REQUEST = "SC_DUPLICATE_REQUEST";
    public static final String CURRENCY_NOT_SUPPORTED = "SC_CURRENCY_NOT_SUPPORTED";
    public static final String UNDER_MAINTENANCE = "SC_UNDER_MAINTENANCE";
    public static final String UNKNOWN_ERROR = "SC_UNKNOWN_ERROR";
    public static final String MISMATCHED_DATA_TYPE = "SC_MISMATCHED_DATA_TYPE";
    public static final String INVALID_VALUE = "Invalid value.";

    public static final Map<String, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Success");
        put(INVALID_REQUEST, "Bad request, please check your post parameters.");
        put(MISMATCHED_DATA_TYPE, "Data type mismatched in one or more parameters.");
        put(AUTHENTICATION_FAILED, "Authentication failed. X-API-Key is missing or invalid.");
        put(INVALID_SIGNATURE, "Invalid signature.");
        put(INVALID_GAME, "Game is not supported.");
        put(DUPLICATE_REQUEST, "Same trace Id found.");
        put(CURRENCY_NOT_SUPPORTED, "Currency is not supported.");
        put(UNDER_MAINTENANCE, "Game is under maintenance.");
        put(UNKNOWN_ERROR, "Internal server error.");
    }};
}
