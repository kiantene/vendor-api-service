package com.nextgen.gameaggregator.vendor.db.constant;

import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class ResponseCodes {

    public static final int PLAYER_NOT_EXIST = 10001;
    public static final int INSUFFICIENT_BALANCE = 10002;
    public static final int INVALID_SIGNATURE = 10003;
    public static final int INTERNAL_SERVER_ERROR = 10004;
    public static final int PLAYER_NOT_EXIST_1 = 10101;
    public static final int INVALID_SIGNATURE_1 = 10102;
    public static final int INTERNAL_SERVER_ERROR_1 = 10103;

    // transaction error response
    public static final int SUCCESS = 1000;
    public static final int INVALID_PARAMETER = 1001;
    public static final int MISSING_PARAMETER = 1002;
    public static final int TIMEOUT = 1003;
    public static final int BET_NOT_FOUND = 10201;
    public static final int INVALID_TOKEN = 2004;
    public static final int INVALID_GAME_ID = 8002;

    public static final Map<Integer, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(PLAYER_NOT_EXIST, "Player does not exist");
        put(INSUFFICIENT_BALANCE, "Insufficient Balance");
        put(INVALID_SIGNATURE, "Invalid Signature");
        put(INTERNAL_SERVER_ERROR, "Internal Server Error");

        put(PLAYER_NOT_EXIST_1, "Player does not exist");
        put(INVALID_SIGNATURE_1, "Invalid Signature");
        put(INTERNAL_SERVER_ERROR_1, "Internal Server Error");

        put(SUCCESS, "SUCCESS");
        put(INVALID_PARAMETER, "Invalid Parameter");
        put(MISSING_PARAMETER, "Missing Parameter");
        put(TIMEOUT, "Timeout");
        put(BET_NOT_FOUND, "Bet Not Found");
        put(INVALID_GAME_ID, "Game does not exist");


    }};

}
