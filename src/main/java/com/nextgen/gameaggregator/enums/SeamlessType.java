package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum SeamlessType {
    SEAMLESS(1, "Seamless"),
    SEAMLESS_TRANSFER(2, "Seamless Transfer");
    public final Integer code;
    public final String description;

    public static boolean isSeamlessTransfer(Integer type) {
        return SEAMLESS_TRANSFER.code.equals(type);
    }
}
