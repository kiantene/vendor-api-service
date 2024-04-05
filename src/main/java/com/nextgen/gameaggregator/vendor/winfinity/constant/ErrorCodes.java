package com.nextgen.gameaggregator.vendor.winfinity.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ErrorCodes {
    BAD_REQUEST(400, "Bad Request"),
    UNKNOWN_ERROR(500, "Unknown Error"),
    REQUEST_TIMEOUT(504, "Request Timeout"),
    NOT_ENOUGH_FUND(600, "Not enough funds for current operation"),
    WRONG_SESSION(601, "Wrong session identifier"),
    PLAYER_NOT_ALLOWED(605, "Players screen name not allowed"),
    PAYIN_TRANS_NOT_FOUND(650, "Could not make PayOut request because PayIn transaction not found"),
    TRANS_ALREADY_EXISTS(651, "Transaction already exists"),
    TRANS_REFUNDED(652, "Transaction already refunded"),
    ROUND_CANCELLED(653, "Round cancelled"),
    GAME_NOT_AVAILABLE(701, "Game is not available right now"),
    CURRENCY_NOT_ALLOWED(702, "Currency is not allowed");

    public final Integer code;
    public final String description;
}
