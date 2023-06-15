package com.nextgen.gameaggregator.vendor.spinix.constant;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {
    public static final String UNEXPECTED_INTERNAL_SERVER_ERROR = "E20000";
    public static final String PARAMETER_INVALID = "E20200";
    public static final String GAME_NOT_FOUND = "E20600";
    public static final String USER_NOT_FOUND = "E20700";
    public static final String USER_TOKEN_NOT_FOUND_OR_INVALID = "E20701";
    public static final String GAME_NOT_AVAILABLE = "E20601";
    public static final String TRANSACTION_INVALID = "E20900";
    public static final String INSUFFICIENT_BALANCE = "E20502";

    public static final Map<String, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(UNEXPECTED_INTERNAL_SERVER_ERROR, "Unexpected internal server error");
        put(PARAMETER_INVALID, "Parameter invalid");
        put(GAME_NOT_FOUND, "Game not found");
        put(USER_NOT_FOUND, "User not found");
        put(USER_TOKEN_NOT_FOUND_OR_INVALID, "User token not found or invalid");
        put(GAME_NOT_AVAILABLE, "Game not available");
        put(TRANSACTION_INVALID, "Transaction Invalid");
        put(INSUFFICIENT_BALANCE, "Balance not enough");
    }};
}
