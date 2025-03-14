package com.nextgen.gameaggregator.vendor.gpkpushgaming.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS(0, "Success"),
    INSUFFICIENT_BALANCE(1085, "The transfer of account failed, as the account balance is insufficient"),
    ERROR(1001, "Error");

    public final Integer code;
    public final String message;

    public static ResponseCodes fromCode(int code) {
        for (ResponseCodes responseCode : ResponseCodes.values()) {
            if (responseCode.code == code) {
                return responseCode;
            }
        }
        throw new IllegalArgumentException("Unknown response code: " + code);
    }
}