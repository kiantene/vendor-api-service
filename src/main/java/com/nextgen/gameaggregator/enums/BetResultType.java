package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum BetResultType {
    /**
     * Result Type used in bet_history table
     */

    BET (1, "Bet"),
    LOSE (2, "Lose"),
    WIN (3, "Win"),
    JACKPOT (4, "Jackpot"),
    ;

    public final Integer code;
    public final String description;
}
