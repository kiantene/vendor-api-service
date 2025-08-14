package com.nextgen.gameaggregator.vendor.marblex.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StatusCode {
    public static final Integer SUCCESS = 10001;
    public static final int INVALID_AUTHENTICATION = 40001;
    public static final int INVALID_REQUEST = 40002;
    public static final int INSUFFICIENT_BALANCE = 40101;
    public static final int TRANSACTION_NOT_FOUND = 40102;
    public static final int CURRENCY_MISMATCH = 40103;
    public static final int INVALID_VENDOR = 40201;
    public static final int INVALID_GAME_CODE = 40202;
    public static final int INVALID_CURRENCY = 40203;
    public static final int INVALID_PLAYER_ID = 40204;
    public static final int INVALID_PLATFORM = 40205;
    public static final int VENDOR_DISABLED = 40301;
    public static final int GAME_DISABLED = 40302;
    public static final int PLAYER_DISABLED = 40303;
    public static final int PLATFORM_DISABLED = 40304;
    public static final int UNKNOWN_ERROR = 50001;
    public static final int VENDOR_API_ERROR = 60001;
    public static final int VENDOR_API_NOT_SUPPORT = 60009;
}
