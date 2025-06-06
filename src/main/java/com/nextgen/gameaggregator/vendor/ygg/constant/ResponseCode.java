package com.nextgen.gameaggregator.vendor.ygg.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS(0, "SC_OK"),
    ERROR_NOT_LOGGED_IN(1000, "NOT_LOGGED_IN"),
    ERROR_OVERDRAFT(1006, "OVERDRAFT"),
    ERROR_BLOCKED(1007, "BLOCKED"),
    ERROR_NOT_AUTHORIZED(1008, "NOT_AUTHORIZED"),
    ERROR_BONUS_LIMIT(1013, "BONUS_LIMIT"),
    ERROR(1, "SC_ERROR");

    public final Integer code;
    public final String message;
}