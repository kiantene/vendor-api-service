package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum BetStatus {
    UNSETTLED (1, "Unsettled bets"),
    SETTLED (2, "Settled bets"),
    CANCELLED (3, "Cancelled bets"),
    REFUNDED (4, "Refunded bets")
    ;

    public final Integer code;
    public final String description;
}
