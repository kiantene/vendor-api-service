package com.nextgen.gameaggregator.operator.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {
    public enum Status {
        SC_OK,
        SC_INVALID_REQUEST,
        SC_AUTHENTICATION_FAILED,
        SC_INVALID_SIGNATURE,
        SC_INVALID_TOKEN,
        SC_INVALID_GAME,
        SC_INVALID_CURRENCY,
        SC_USER_NOT_EXISTS,
        SC_DUPLICATE_REQUEST,
        SC_CURRENCY_NOT_SUPPORTED,
        SC_UNDER_MAINTENANCE,
        SC_UNKNOWN_ERROR,
        SC_MISMATCHED_DATA_TYPE,
        SC_INSUFFICIENT_FUNDS
    }

    public static final Map<Status, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(Status.SC_OK, "Success");
        put(Status.SC_INVALID_REQUEST, "Bad request, please check your post parameters.");
        put(Status.SC_MISMATCHED_DATA_TYPE, "Data type mismatched in one or more parameters.");
        put(Status.SC_AUTHENTICATION_FAILED, "Authentication failed. X-API-Key is missing or invalid.");
        put(Status.SC_INVALID_SIGNATURE, "Invalid signature.");
        put(Status.SC_INVALID_TOKEN, "Invalid token.");
        put(Status.SC_INVALID_GAME, "Game is not supported.");
        put(Status.SC_USER_NOT_EXISTS, "User does not exists.");
        put(Status.SC_DUPLICATE_REQUEST, "Same trace Id found.");
        put(Status.SC_CURRENCY_NOT_SUPPORTED, "Currency is not supported.");
        put(Status.SC_UNDER_MAINTENANCE, "Game is under maintenance.");
        put(Status.SC_UNKNOWN_ERROR, "Internal server error.");
    }};
}
