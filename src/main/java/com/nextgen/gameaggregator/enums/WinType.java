package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum WinType {
    LOSE (1, "Lose"),
    WIN (2, "Win"),
    JACKPOT (3, "Jackpot"),

    PENDING (99, "Pending")
    ;

    public final Integer code;
    public final String description;
}
