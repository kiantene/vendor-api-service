package com.nextgen.gameaggregator.vendor.aviatorstudio.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {

    SUCCESS(200, "Success"),
    AUTH_ERROR(403, "Authentication failed"),
    SERVER_ERROR(500, "Server error");

    public final Integer code;
    public final String description;
}
