package com.nextgen.gameaggregator.operator.constant;

import lombok.AllArgsConstructor;

public class SportBetStatus {
    @AllArgsConstructor
    public enum BetStatus {
        HALF_WIN("HALF_WIN"),
        HALF_LOSE("HALF_LOSE"),

        WIN("WIN"),
        LOSE("LOSE"),
        DRAW("DRAW"),
        RUNNING("RUNNING"),

        CANCELLED("CANCELLED"),
        REFUNDED("REFUNDED"),
        PENDING("PENDING"),
        REJECTED("REJECTED"),
        ;
        public final String value;

    }
}

