package com.nextgen.gameaggregator.vendor.koolbet.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS(0, "SC_OK"),
    ERROR(103, "SC_ERROR"),
    CREDENTIAL_ERROR(10303, "INVALID_CREDENTIALS"),
    ERROR_OVERDRAFT(10805, "INSUFFICIENT_FUNDS"),
    ERROR_BLOCKED(10410, "BLOCKED"),
    ERROR_NOT_AUTHORIZED(10501, "NOT_AUTHORIZED");

    public final Integer code;
    public final String message;
}
