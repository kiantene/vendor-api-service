package com.nextgen.gameaggregator.vendor.dotconnections.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {
    public static final String SUCCESS = "1000";
    public static final String SYSTEM_ERROR = "1001";
    public static final String UNKNOWN = "1002";
    public static final String SIGN_ERROR = "5000";
    public static final String REQUEST_PARAM_ERROR = "5001";
    public static final String CURRENCY_NOT_SUPPORT = "5002";
    public static final String BALANCE_INSUFFICIENT = "5003";
    public static final String BRAND_NOT_EXIST = "5005";
    public static final String COUNTRY_CODE_ERROR = "5008";
    public static final String PLAYER_NOT_EXIST = "5009";
    public static final String PLAYER_BLOCK = "5010";
    public static final String GAME_ID_NOT_EXIST = "5012";
    public static final String NOT_LOGGED_IN = "5013";
    public static final String INVALID_TIME_FORMAT = "5014";
    public static final String INVALID_PROVIDER = "5015";
    public static final String INVALID_AMOUNT = "5016";
    public static final String API_INSUFFICIENT_PERMISSION = "5017";
    public static final String INVALID_BRAND_UID = "5018";
    public static final String RATE_LIMIT_EXCEEDED = "5040";
    public static final String REQUEST_DATE_RANGE_LIMIT_EXCEEDED = "5041";
    public static final String BET_RECORD_NOT_EXIST = "5042";
    public static final String BET_RECORD_DUPLICATE = "5043";
    public static final String FREE_SPIN_ID_NOT_EXIST = "5070";
    public static final String INVALID_ROUND_COUNT = "5071";
    public static final String FREE_SPIN_CANCELLED = "5072";
    public static final String FREE_SPIN_LOCKED = "5073";
    public static final String FREE_SPIN_NOT_SUPPORT = "5074";
    public static final String FREE_SPIN_SET_UP_ERROR = "5075";

    public static final Map<String, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Success.");
        put(SYSTEM_ERROR, "System error.");
        put(UNKNOWN, "Unknown.");
        put(SIGN_ERROR, "Sign error.");
        put(REQUEST_PARAM_ERROR, "Request param error.");
        put(CURRENCY_NOT_SUPPORT, "Currency not support.");
        put(BALANCE_INSUFFICIENT, "Balance insufficient.");
        put(BRAND_NOT_EXIST, "Brand not exist.");
        put(COUNTRY_CODE_ERROR, "Country Code error.");
        put(PLAYER_NOT_EXIST, "Player not exist.");
        put(PLAYER_BLOCK, "Player blocked.");
        put(GAME_ID_NOT_EXIST, "Game id not exist.");
        put(NOT_LOGGED_IN, "Not logged in.");
        put(INVALID_TIME_FORMAT, "Incorrect time format.");
        put(INVALID_PROVIDER, "Incorrect provider.");
        put(INVALID_AMOUNT, "Incorrect amount.");
        put(API_INSUFFICIENT_PERMISSION, "Api insufficient permission.");
        put(INVALID_BRAND_UID, "Incorrect brand uid.");
        put(RATE_LIMIT_EXCEEDED, "Request rate limit, once request per 3 seconds.");
        put(REQUEST_DATE_RANGE_LIMIT_EXCEEDED, "Request date range limit, once request time period can only be within 24 hours. request time can only be within 6 months.");
        put(BET_RECORD_NOT_EXIST, "Bet record not exist.");
        put(BET_RECORD_DUPLICATE, "Bet record duplicate.");
        put(FREE_SPIN_ID_NOT_EXIST, "Free spin ID not exist.");
        put(INVALID_ROUND_COUNT, "Incorrect round count.");
        put(FREE_SPIN_CANCELLED, "The free spin already cancelled.");
        put(FREE_SPIN_LOCKED, "The free spin already locked.");
        put(FREE_SPIN_NOT_SUPPORT, "The provider does not support free spin.");
        put(FREE_SPIN_SET_UP_ERROR, "The free spin set up error.");
    }};
}
