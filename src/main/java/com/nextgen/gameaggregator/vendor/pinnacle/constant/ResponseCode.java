package com.nextgen.gameaggregator.vendor.pinnacle.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS             (0, "Success"),
    UNKNOWN_ERROR       (-1, "Unknown Error"),
    ACC_NOT_FOUND       (-5, "Account Not Found"),
    AUTH_FAILED         (-6, "API Authentication failed");

    public final Integer code;
    public final String description;
}
