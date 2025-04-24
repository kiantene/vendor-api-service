package com.nextgen.gameaggregator.vendor.amusnet.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {

    OK(1000, "OK"),
    DUPLICATE(1100, "Duplicate"),
    TIME_OUT(2000, "Time Out"),
    INTERNAL_SERVER_ERROR(3000, "Internal Server Error"),
    INSUFFICIENT_FUNDS(3100, "Insufficient Funds"),
    DEFENCE_CODE_ERROR(3100, "Expired Defence code");

    public final Integer errorCode;
    public final String errorMessage;
}
