package com.nextgen.gameaggregator.vendor.facai.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {

    public static final String SUCCESS = "0";
    public static final String INSUFFICIENT_BALANCE = "203";
    public static final String GAME_NOT_FOUND = "405";
    public static final String PLAYER_NOT_FOUND = "500";
    public static final String REVERT_CANCEL_BET = "799";
    public static final String TRANSACTION_NOT_EXIST = "221";
    public static final String REQUIRE_CANCEL_REQUEST = "899";
    public static final String UNEXPECTED_ERROR = "999";
    public static final String CURRENCY_MISSING = "1012";
    public static final String DATE_INPUT_MISSING = "1018";
    public static final String GAME_TYPE_MISSING = "1019";
    public static final String PARAM_CONTAIN_ERROR = "1099";

    public static final Map<String, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Success");
        put(INSUFFICIENT_BALANCE, "Your points balance not enough.");
        put(GAME_NOT_FOUND, "Game does not exist.");
        put(PLAYER_NOT_FOUND, "Account does not exist.");
        put(REVERT_CANCEL_BET, "Revert Cancel Bet.");
        put(TRANSACTION_NOT_EXIST, "Transaction ID number not exist.");
        put(REQUIRE_CANCEL_REQUEST, "Require to send Cancel request.");
        put(UNEXPECTED_ERROR, "Unexpected error.");
        put(CURRENCY_MISSING, "Currency code is missing.");
        put(DATE_INPUT_MISSING, "Date input is missing.");
        put(GAME_TYPE_MISSING, "Game type is missing.");
        put(PARAM_CONTAIN_ERROR, "The parameter contain error, please check the parameter is correct or not.");
    }};


}
