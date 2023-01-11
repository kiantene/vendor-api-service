package com.nextgen.gameaggregator.operator.constant;

import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;


public class ResponseCodes {
    @AllArgsConstructor
    public enum Status {
        SC_OK (1, "Success"),
        SC_INVALID_REQUEST (2, "Bad request, please check your post parameters."),
        SC_AUTHENTICATION_FAILED (3, "Authentication failed. X-API-Key is missing or invalid."),
        SC_INVALID_SIGNATURE (4, "Invalid signature."),
        SC_INVALID_TOKEN (5, "Invalid token."),
        SC_INVALID_GAME (6, "Game is not supported."),
        SC_INVALID_CURRENCY (7, "Currency is not supported"),
        SC_USER_NOT_EXISTS (8, "User does not exists."),
        SC_DUPLICATE_REQUEST (9, "Duplicate request."),
        SC_CURRENCY_NOT_SUPPORTED (10, "Currency is not supported."),
        SC_UNDER_MAINTENANCE (11, "Game is under maintenance."),
        SC_UNKNOWN_ERROR (12, "Internal server error."),
        SC_MISMATCHED_DATA_TYPE (13, "Invalid data type."),
        SC_INSUFFICIENT_FUNDS (14, "Insufficient funds.")
        ;

        public final Integer code;
        public final String description;
    }
}
