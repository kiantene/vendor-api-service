package com.nextgen.gameaggregator.vendor.tbp.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    UNAUTHORIZED(1, "Unauthorized"),
    PERMISSION_DENIED(2, "PermissionDenied"),
    BAD_INPUT(3, "BadInput"),
    UNEXPECTED_INPUT(4, "UnexpectedInput"),
    UNAVAILABLE(6, "Unavailable"),
    OK(1000, "OK"),
    TIMEOUT(2000, "Time out"),
    INTERNAL_SERVER_ERROR(3000, "Internal server error"),
    EXPIRED(3100, "Expired"),
    INSUFFICIENT_FUNDS(3200, "Insufficient funds"),
    LOSS_LIMIT_EXCEEDED(3400, "Loss limit exceeded");

    public final int code;
    public final String description;
}