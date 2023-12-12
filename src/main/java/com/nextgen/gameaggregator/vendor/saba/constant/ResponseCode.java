package com.nextgen.gameaggregator.vendor.saba.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS("0", null),
    SYSTEM_ERROR_RETRY("999", "System Error");

    public final String status;
    public final String message;
}
