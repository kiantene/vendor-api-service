package com.nextgen.gameaggregator.vendor.hacksaw.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS(0, "Success"),
    GENERAL_OR_SERVER_ERROR(1, "General/Server error"), //For rollback
    INVALID_USER_OR_TOKEN_EXPIRED(2, "Invalid user / token expired"),
    INVALID_CURRENCY(3, "Invalid currency for user"),
    INVALID_PARTNER_CODE(4, "Invalid partner code"),
    INSUFFICIENT_FUNDS(5, "Insufficient funds to place bet"),
    ACCOUNT_LOCKED(6, "Account locked"),
    ACCOUNT_DISABLED(7, "Account disabled"),
    GAMBLING_LIMIT_EXCEEDED(8, "Gambling limit exceeded (Loss limit or betting limit)"),
    TIME_LIMIT_EXCEEDED(9, "Time limit exceeded"),
    SESSION_TIMEOUT(10, "Session timeout or invalid session id"),
    GENERAL_ERROR(11, "General error"),
    INVALID_ACTION(12, "Invalid action");

    public final Integer statusCode;
    public final String statusMessage;
}
