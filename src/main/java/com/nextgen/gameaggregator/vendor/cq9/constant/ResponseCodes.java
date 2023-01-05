package com.nextgen.gameaggregator.vendor.cq9.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {
    public static final String SUCCESS = "0";
    public static final String GAME_ACTION_ERROR = "1002";
    public static final String PARAMETER_ERROR = "1003";
    public static final String TIME_FORMAT_ERROR = "1004";
    public static final String INSUFFICIENT_BALANCE = "1005";
    public static final String PLAYER_NOT_FOUND = "1006";
    public static final String TRANSACTION_RECORD_NOT_FOUND = "1014";
    public static final String SERVER_ERROR = "1100";

    public static final Map<String, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Success");
        put(GAME_ACTION_ERROR, "Game action error.");
        put(PARAMETER_ERROR, "Parameter error.");
        put(TIME_FORMAT_ERROR, "Time Format error.");
        put(INSUFFICIENT_BALANCE, "Insufficient Balance.");
        put(PLAYER_NOT_FOUND, "Player not found.");
        put(TRANSACTION_RECORD_NOT_FOUND, "Transaction record not found.");
        put(SERVER_ERROR, "Server error.");
    }};
}
