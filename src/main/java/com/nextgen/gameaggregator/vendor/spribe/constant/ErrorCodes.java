package com.nextgen.gameaggregator.vendor.spribe.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ErrorCodes {
    SUCCESS                     (200, "Success"),
    INVALID_TOKEN               (401, "User token is invalid"),
    INSUFFICIENT_FUND           (402, "Insufficient fund"),
    EXPIRED_TOKEN               (403, "User token is expired"),
    INTERNAL_ERROR_NO_RETRY     (405, "Internal error with no retry"),
    TRANSACTION_NOT_FOUND       (408, "Transaction does not found"),
    DUPLICATE_TRANSACTION       (409, "Duplicate transaction"),
    INTERNAL_ERROR              (500, "Internal error");

    public final Integer code;
    public final String description;
}

