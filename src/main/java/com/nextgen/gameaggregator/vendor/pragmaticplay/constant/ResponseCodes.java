package com.nextgen.gameaggregator.vendor.pragmaticplay.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {
    public static final Integer SUCCESS = 0;
    public static final Integer INSUFFICIENT_BALANCE = 1;
    public static final Integer PLAYER_NOT_FOUND = 2;
    public static final Integer BET_NOT_ALLOWED = 3;
    public static final Integer AUTHENTICATION_ERROR = 4;
    public static final Integer INVALID_HASH = 5;
    public static final Integer PLAYER_FROZEN = 6;
    public static final Integer INVALID_REQUEST = 7;
    public static final Integer INVALID_GAME = 8;
    public static final Integer INTERNAL_SERVER_ERROR_RETRY = 100;
    public static final Integer INTERNAL_SERVER_ERROR_NO_RETRY = 120;

    public static final Map<Integer, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Success");
        put(INSUFFICIENT_BALANCE, "Insufficient balance.");
        put(PLAYER_NOT_FOUND, "Player not found or is logged out.");
        put(BET_NOT_ALLOWED, "Bet is not allowed.");
        put(AUTHENTICATION_ERROR, "Player authentication failed due to invalid, not found or expired token.");
        put(INVALID_HASH, "Invalid hash code.");
        put(PLAYER_FROZEN, "Player is frozen.");
        put(INVALID_REQUEST, "Bad parameters in the request, please check post parameters.");
        put(INVALID_GAME, "Game is not found or disabled.");
        put(INTERNAL_SERVER_ERROR_RETRY, "Internal server error. Please retry.");
        put(INTERNAL_SERVER_ERROR_NO_RETRY, "Internal server error.");
    }};
}
