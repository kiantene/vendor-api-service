package com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant;

import java.util.HashMap;
import java.util.Map;

public class ConstantErrorMessage {
    //* PP error codes
    public static final Integer CODE_SUCCESS = 0;
    public static final Integer CODE_UNEXPECTED_ERROR = 1;
    public static final Integer CODE_INVALID_PARAM = 2;

    public static final String RESPONSE_KEY_SUCCESS = "SUCCESS";
    public static final String RESPONSE_KEY_INVALID_PARAM = "INVALID_PARAM";
    public static final String RESPONSE_KEY_PLAYER_AUTH_FAILED = "PLAYER_AUTH_FAILED";
    public static final String RESPONSE_KEY_PLAYER_FROZEN = "PLAYER_FROZEN";
    public static final String RESPONSE_KEY_GAME_NOT_FOUND = "GAME_NOT_FOUND";
    public static final String RESPONSE_KEY_INTERNAL_SERVER_ERROR_RECONCILIATION = "INTERNAL_SERVER_ERROR_RECONCILIATION";
    public static final String RESPONSE_KEY_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final Map<String, Integer> RESPONSE_CODES = new HashMap<String, Integer>() {{
        put(RESPONSE_KEY_SUCCESS, 0);
        put(RESPONSE_KEY_PLAYER_AUTH_FAILED, 4);
        put(RESPONSE_KEY_PLAYER_FROZEN, 6);
        put(RESPONSE_KEY_GAME_NOT_FOUND, 8);
        put(RESPONSE_KEY_INTERNAL_SERVER_ERROR_RECONCILIATION, 100);
        put(RESPONSE_KEY_INTERNAL_SERVER_ERROR, 120);
    }};

    //* PP error messages
    public static final Map<String, String> RESPONSE_MESSAGES = new HashMap<String, String>() {{
        put(RESPONSE_KEY_SUCCESS, "Success");
        put(RESPONSE_KEY_PLAYER_AUTH_FAILED, "Player authentication failed due to invalid, not found or expired token");
        put(RESPONSE_KEY_PLAYER_FROZEN, "");
        put(RESPONSE_KEY_GAME_NOT_FOUND, "");
        put(RESPONSE_KEY_INTERNAL_SERVER_ERROR_RECONCILIATION, "Internal server error, please try again later");
        put(RESPONSE_KEY_INTERNAL_SERVER_ERROR, "Internal server error, please try again later");
    }};
    public static final String MESSAGE_UNEXPECTED_ERROR = "Unexpected UNEXPECTED";



    //region COMMON ERROR
    public static final String NOT_NULL = "cannot be empty";
    public static final String NOT_BLANK = "cannot be blank";
    public static final String POSITIVE = " must be positive number";

    public static final String SIZE_MIN_MAX = " must be between";
    public static final String EXCEED_MAX = "cannot more than";
    public static final String MIN_REQUIRED = "cannot less than";

    public static final String NOT_INTEGER = "must be a number";

    public static final String NOT_FOUND = "not found";

    public static final String NOT_SUPPORT = "not support";

    public static final String DISABLE = "was disable";


    //endregion


    public static final String VENDOR_GROUP_NOT_VALID = "'s vendor group not valid";

    public static final String VENDOR_GROUP_DISABLE = "'s vendor group was disable";

    public static final String INVALID_TRACE_ID = "invalid UUID format";
}
