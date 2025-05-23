package com.nextgen.gameaggregator.vendor.tbp.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    UNAUTHORIZED(1, "Used authorization is insufficient"),
    PERMISSION_DENIED(2, "Used authorization is sufficient, but the values do not match system ones"),
    BAD_INPUT(3, "Missing mandatory information"),
    UNEXPECTED_INPUT(4, "Submitted values do not match the expected input type"),
    UNAVAILABLE(6, "Temporarily unavailable service, usually due to maintenance"),
    OK(1000, "No errors were encountered"),
    TIMEOUT(2000, "Connection timed out"),
    INTERNAL_SERVER_ERROR(3000, "Unknown internal server error"),
    EXPIRED(3100, "Session expired"),
    INSUFFICIENT_FUNDS(3200, "The user does not have enough funds for a bet"),
    LOSS_LIMIT_EXCEEDED(3400, "The user has exceeded the loss limit"),
    UNKNOWN(-1, "Unknown error");

    public final int code;
    public final String description;
}