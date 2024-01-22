package com.nextgen.gameaggregator.operator.constant;

import lombok.AllArgsConstructor;

public class SportBetStatus {
    @AllArgsConstructor
    public enum BetStatus {
        WIN("WIN"),
        LOSE("LOSE"),
        CANCELLED("CANCELLED"),
        REFUNDED("REFUNDED"),
        PENDING("PENDING"),
        ;
        public final String value;

    }
}

