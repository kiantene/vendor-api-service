package com.nextgen.gameaggregator.vendor.pragmaticplay.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS                 (0, "Success"),
    INSUFFICIENT_BALANCE    (1, "Insufficient balance."),
    PLAYER_NOT_FOUND        (2, "Player not found or is logged out."),
    BET_NOT_ALLOWED         (3, "Bet is not allowed."),
    AUTHENTICATION_ERROR    (4, "Player authentication failed due to invalid, not found or expired token."),
    INVALID_HASH            (5, "Invalid hash code."),
    PLAYER_FROZEN           (6, "Player is frozen."),
    INVALID_REQUEST         (7, "Bad parameters in the request, please check post parameters."),
    INVALID_GAME            (8, "Game is not found or disabled."),
    INTERNAL_SERVER_ERROR_RETRY     (100, "Internal server error. Please retry."),
    INTERNAL_SERVER_ERROR_NO_RETRY  (120, "Internal server error.")
    ;

    public final Integer code;
    public final String description;
}
