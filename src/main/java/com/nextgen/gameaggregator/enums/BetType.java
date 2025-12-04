package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum BetType {
    NORMAL_BET              (1, "normal bet"),
    PARLAY_BET              (2, "parlay bet"),
    SIDE_BET                (3, "Side Bet"),
    TWENTY_ONE_PLUS_THREE   (4, "21+3"),
    PERFECT_PAIR            (5, "Perfect Pair"),
    ANY_PAIR                (6, "Any Pair"),
    BUST_IT                 (7, "Bust It"),
    HOT_3                   (8, "Hot 3"),
    MAIN_BET                (9, "Main Bet"),
    LUCKY_LADIES            (10, "Lucky Ladies"),
    TEN_20                  (11, "Ten 20"),
    PERFECT_11              (12, "Perfect 11"),
    RAZZ_BONUS              (13, "Razz Bonus")
    ;

    public final Integer code;
    public final String description;

    public boolean isValueOf(Integer status) {
        return status.equals(code);
    }
}
