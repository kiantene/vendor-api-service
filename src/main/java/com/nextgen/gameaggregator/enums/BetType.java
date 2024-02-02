package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum BetType {
    NORMAL_BET(1, "normal bet"),
    PARLAY_BET(2, "parlay bet");

    public final Integer code;
    public final String description;

    public boolean isValueOf(Integer status) {
        return status.equals(code);
    }
}
