package com.nextgen.gameaggregator.vendor.ezugi.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {
    public static final Integer COMPLETED_SUCCESSFULLY = 0;
    public static final Integer GENERAL_ERROR = 1;
    public static final Integer SAVED_FOR_FUTURE_USE = 2;
    public static final Integer INSUFFICIENT_FUNDS = 3;
    public static final Integer OPERATOR_LIMIT_TO_THE_PLAYER_1 = 4;
    public static final Integer OPERATOR_LIMIT_TO_THE_PLAYER_2 = 5;
    public static final Integer TOKEN_NOT_FOUND = 6;
    public static final Integer USER_NOT_FOUND = 7;
    public static final Integer USER_BLOCKED = 8;
    public static final Integer TRANSACTION_NOT_FOUND = 9;
    public static final Integer TRANSACTION_TIMED_OUT = 10;
    public static final Integer REAL_BALANCE_IS_NOT_ENOUGH_FOR_TIPPING = 11;

    public static final Map<Integer, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(COMPLETED_SUCCESSFULLY, "Completed successfully.");
        put(GENERAL_ERROR, "General error.");
        put(SAVED_FOR_FUTURE_USE, "Saved for future use.");
        put(INSUFFICIENT_FUNDS, "Insufficient funds.");
        put(OPERATOR_LIMIT_TO_THE_PLAYER_1, "Operator limit to the player 1.");
        put(OPERATOR_LIMIT_TO_THE_PLAYER_2, "Operator limit to the player 2.");
        put(TOKEN_NOT_FOUND, "Token not found.");
        put(USER_NOT_FOUND, "User not found.");
        put(USER_BLOCKED, "User blocked.");
        put(TRANSACTION_NOT_FOUND, "Transaction not found.");
        put(TRANSACTION_TIMED_OUT, "Transaction timed out.");
        put(REAL_BALANCE_IS_NOT_ENOUGH_FOR_TIPPING, "Real balance is not enough for tipping.");
    }};
}
