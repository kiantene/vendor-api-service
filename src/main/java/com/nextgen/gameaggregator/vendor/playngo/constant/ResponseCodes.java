package com.nextgen.gameaggregator.vendor.playngo.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {

    public static final String SUCCESS = "0";
    public static final String IP_NOT_ALLOWED = "1";
    public static final String INVALID_APPID = "2";
    public static final String INVALID_TOKEN = "3";
    public static final String INVALID_PARAMETERS = "4";
    public static final String INVALID_SIGNATURE = "5";
    public static final String INVALID_TIMESTAMP = "6";
    public static final String INVALID_USERNAME_PASSWORD = "7";
    public static final String INSUFFICIENT_FUND = "100";
    public static final String TRANSACTION_IS_PROCESSED = "201";
    public static final String SERVER_MAINTENANCE = "999";
    public static final String OTHER_MESSAGE = "1000";

    public static final Map<String, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Success");
        put(IP_NOT_ALLOWED, "IP not allowed");
        put(INVALID_APPID, "Invalid AppID");
        put(INVALID_TOKEN, "Invalid token");
        put(INVALID_PARAMETERS, "Invalid parameters");
        put(INVALID_SIGNATURE, "Invalid signature");
        put(INVALID_TIMESTAMP, "Invalid timestamp");
        put(INVALID_USERNAME_PASSWORD, "Invalid Username or Password");
        put(INSUFFICIENT_FUND, "Insufficient fund");
        put(TRANSACTION_IS_PROCESSED, "Transaction is being processed");
        put(SERVER_MAINTENANCE, "Server under maintenance");
        put(OTHER_MESSAGE, "Other");
    }};


}
