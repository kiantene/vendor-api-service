package com.nextgen.gameaggregator.vendor.cpgame.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {
    public static final int SUCCESS = 0;

    public static final int UNKNOWN_ERROR = 1199;

    public static final int SYSTEM_BUSY = 1002;

    public static final int GAME_KEY_ERROR = 1003;

    public static final int SUB_UID_ERROR = 1004;

    public static final int INVALID_REQUEST = 1110;

    public static final int SIGNATURE_ERROR = 1111;

    public static final int APP_ID_ERROR = 1113;

    public static final int GAME_ID_ERROR = 1115;

    public static final int PLAYER_NOT_EXIST = 1116;

    public static final int INSUFFICIENT_BALANCE = 1117;

    public static final int TRANSACTION_NOT_EXIST = 1118;
    public static final int BET_NOT_FOUND = 1119;

    public static final Map<Integer, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Success");
        put(INSUFFICIENT_BALANCE, "Insufficient Balance");
        put(PLAYER_NOT_EXIST, "Invalid Player");
        put(UNKNOWN_ERROR, "Unknown Error");
        put(INVALID_REQUEST, "Invalid Request");
        put(SYSTEM_BUSY, "System Busy");
        put(SIGNATURE_ERROR, "Signature Error");
        put(GAME_KEY_ERROR, "Game Key Error");
        put(TRANSACTION_NOT_EXIST, "Transaction Not Exist");
        put(BET_NOT_FOUND, "Bet Not Found");
        put(GAME_ID_ERROR, "Game Code Error");
        put(APP_ID_ERROR, "App Id Error");
        put(SUB_UID_ERROR, "Player Not Exist");
    }};
}
