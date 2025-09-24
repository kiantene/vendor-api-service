package com.nextgen.gameaggregator.vendor.smartsoft.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    //General
    LOSS_LIMIT(112, "Return if loss exceeds limit set on operator's side"),
    INTERNAL_ERROR(500, "Return in case of internal error if that error is not described by code listed in this table"),
    RETRY(-17, "To cause a retry");

    public final Integer code;
    public final String message;

    public static ResponseCode fromCode(int code) {
        for (ResponseCode responseCode : ResponseCode.values()) {
            if (responseCode.code == code) {
                return responseCode;
            }
        }
        throw new IllegalArgumentException("Unknown response code: " + code);
    }
}
