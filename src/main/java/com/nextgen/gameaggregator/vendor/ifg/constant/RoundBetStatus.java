package com.nextgen.gameaggregator.vendor.ifg.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum RoundBetStatus {
    FINISHED("1"),
    UNFINISHED("0");

    public final String code;
}
