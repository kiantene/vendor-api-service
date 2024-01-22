package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum BetType {
    NORMAL_BET (1, "normal bet"),
    PARLEY_BET (2, "parley bet")
    ;

    public final Integer code;
    public final String description;

    public boolean isValueOf(Integer status) {
        return status.equals(code);
    }
}
