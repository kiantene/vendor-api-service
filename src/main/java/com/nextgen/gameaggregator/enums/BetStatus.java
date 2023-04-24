package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum BetStatus {
    UNSETTLED (0, "Unsettled bets"),
    SETTLED (1, "Settled bets"),
    CANCELLED (2, "Cancelled bets"),
    REFUNDED (3, "Refunded bets")
    ;

    public final Integer code;
    public final String description;

    public boolean isValueOf(Integer status) {
        return status.equals(code);
    }
}
