package com.nextgen.gameaggregator.vendor.jdb.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCode {

    public static final String SUCCESS = "0000";
    public static final String INSUFFICIENT_BALANCE = "6006";
    public static final String PLAYER_NOT_FOUND = "7501";
    public static final String INVALID_REQUEST_PARAMETER = "8000";
    public static final String PARAMETER_CANNOT_BE_NEGATIVE = "8003";
    public static final String WRONG_DATE_FORMAT = "8005";
    public static final String NO_AUTHORIZED = "9001";
    public static final String INVALID_ACTION = "9007";
    public static final String DUPLICATE_TRANSACTION = "9011";
    public static final String FAILED = "9999";

    public static final Map<String, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Succeed");
        put(INSUFFICIENT_BALANCE, "Your Cash Balance not enough.");
        put(PLAYER_NOT_FOUND, "User ID cannot be found.");
        put(INVALID_REQUEST_PARAMETER, "The parameter of input error, please check your parameter is correct or not.");
        put(PARAMETER_CANNOT_BE_NEGATIVE, "The parameter cannot be negative.");
        put(WRONG_DATE_FORMAT, "Wrong date format.");
        put(NO_AUTHORIZED, "No authorized to access.");
        put(INVALID_ACTION, "Unknown action.");
        put(DUPLICATE_TRANSACTION, "Duplicate transactions.");
        put(FAILED, "Failed.");
    }};
}
