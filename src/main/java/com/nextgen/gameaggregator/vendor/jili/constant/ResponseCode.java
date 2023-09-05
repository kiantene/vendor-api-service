package com.nextgen.gameaggregator.vendor.jili.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS(0, "Success"),
    ALREADY_ACCEPTED(1, "Already accepted"),
    ROUND_NOT_FOUND(2, "Round not found"),
    NOT_ENOUGH_BALANCE(2, "Not enough balance"),
    INVALID_PARAMETER(3, "Invalid parameter"),
    TOKEN_EXPIRED(4, "Token expired"),
    OTHER_ERROR(5, "Other error"),
    ALREADY_ACCEPTED_AND_CANNOT_BE_CANCELED(6, "Already accepted and cannot be canceled");

    public final Integer errorCode;
    public final String message;
}
