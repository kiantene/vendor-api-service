package com.nextgen.gameaggregator.vendor.cg.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {

    public static final Integer SUCCESS = 0;
    public static final Integer CHANNEL_ID_ERROR = 8;
    public static final Integer INSUFFICIENT_BALANCE = 10;
    public static final Integer WRONG_URL = 11;
    public static final Integer ACCOUNT_FREEZE = 12;
    public static final Integer REGISTER_ACCOUNT_EXISTED = 13;
    public static final Integer CURRENCY_NOT_SUPPORTED = 14;
    public static final Integer GAMETYPE_ERROR = 16;
    public static final Integer INPUT_ERROR = 26;
    public static final Integer SEAMLESS_INSUFFICIENT_BALANCE = 101;
    public static final Integer SEAMLESS_UNKNOWN_PLAYER = 102;
    public static final Integer SEAMLESS_UNKNOWN_TRANSACTION = 103;
    public static final Integer SEAMLESS_MTCODE_REFUNDED = 104;
    public static final Integer SEAMLESS_MTCODE_REPEAT = 105;
    public static final Integer SEAMLESS_SERVER_ERROR = 106;
    public static final Integer SEAMLESS_INPUT_ERROR = 107;
    public static final Integer SEAMLESS_TIME_FORMAT_ERROR = 108;
    public static final Integer UNKNOWN_ERROR = 999;

    public static final Map<Integer, String> RESPONSE_MESSAGE = new HashMap<>() {{
        put(SUCCESS, "Success");
        put(CHANNEL_ID_ERROR, "Channel Id error");
        put(INSUFFICIENT_BALANCE, "Insufficient balance in this account");
        put(WRONG_URL, "URL error");
        put(ACCOUNT_FREEZE, "Account currently freeze");
        put(REGISTER_ACCOUNT_EXISTED, "Register failed! account ID existed");
        put(CURRENCY_NOT_SUPPORTED, "Only USD is supported in this line");
        put(GAMETYPE_ERROR, "Game code error");
        put(INPUT_ERROR, "Parameters empty or wrong format");
        put(SEAMLESS_INSUFFICIENT_BALANCE, "Seamless wallet insufficient balance");
        put(SEAMLESS_UNKNOWN_PLAYER, "Seamless wallet user not exist");
        put(SEAMLESS_UNKNOWN_TRANSACTION, "Seamless wallet transaction not exist");
        put(SEAMLESS_MTCODE_REFUNDED, "Refund process completed");
        put(SEAMLESS_MTCODE_REPEAT, "Mtcode repeated");
        put(SEAMLESS_SERVER_ERROR, "Seamless wallet server error");
        put(SEAMLESS_INPUT_ERROR, "Wallet parameters empty or wrong format");
        put(SEAMLESS_TIME_FORMAT_ERROR, "Wrong time format");
        put(UNKNOWN_ERROR, "Unknown Error");
    }};
}
