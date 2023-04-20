package com.nextgen.gameaggregator.vendor.queenmaker.constant;


import java.util.LinkedHashMap;
import java.util.Map;

public class ResponseCode {
    public static final String INVALID_OR_EXPIRED_TOKEN = "10";
    public static final String INVALID_TOKEN_SCOPE = "19";
    public static final String USER_BLOCKED = "20";
    public static final String INVALID_CREDENTIAL = "30";
    public static final String CURRENCY_MISMATCH = "50";
    public static final String INSUFFICIENT_FUNDS = "100";
    public static final String INVALID_ARGUMENTS = "300";
    public static final String MISSING_TOKEN = "400";
    public static final String INCORRECT_FORMAT = "500";
    public static final String TRANSACTION_DOES_NOT_EXIST = "600";
    public static final String TRANSACTION_ALREADY_CANCELLED = "610";
    public static final String OPERATION_FAILED_DETERMINISTICALLY = "800";
    public static final String SYSTEM_ERROR = "900";
    public static final String CONFIGURED_TIMEOUT_EXCEEDED = "903";

    public static final Map<String, String> RESPONSE_DESCRIPTION = new LinkedHashMap<>() {{
        put(INVALID_OR_EXPIRED_TOKEN, "Invalid or expired token");
        put(INVALID_TOKEN_SCOPE, "Invalid token scope");
        put(USER_BLOCKED, "User blocked");
        put(INVALID_CREDENTIAL, "Invalid Credential");
        put(CURRENCY_MISMATCH, "Currency Mismatch");
        put(INSUFFICIENT_FUNDS, "Insufficient funds to perform the operation");
        put(INVALID_ARGUMENTS, "Invalid arguments: {argument name or reason}");
        put(MISSING_TOKEN, "Missing token");
        put(INCORRECT_FORMAT, "Incorrect format. {further detail if any}");
        put(TRANSACTION_DOES_NOT_EXIST, "Transaction does not exist");
        put(TRANSACTION_ALREADY_CANCELLED, "Transaction already cancelled");
        put(OPERATION_FAILED_DETERMINISTICALLY, "Operation Failed Deterministically");
        put(SYSTEM_ERROR, "System Error. {further detail if any}");
        put(CONFIGURED_TIMEOUT_EXCEEDED, "Configured Timeout Exceeded");
    }};
    public static final String INVALID_ARGUMENTS_REPLACE_STRING = "{argument name or reason}";
    public static final String INCORRECT_FORMAT_REPLACE_STRING = "{further detail if any}";
    public static final String SYSTEM_ERROR_REPLACE_STRING = "{further detail if any}";

}

