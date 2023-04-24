package com.nextgen.gameaggregator.operator.enums;

import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public enum ResultType {
    /**
     * Result Type that we will send to operator to identify the status of a bet.
     * Possible values are BET, LOSE, WIN, BET_LOSE, BET_WIN, JACKPOT, BET_JACKPOT AND END
     */

    //BET will create a bet data, and cash out if betAmount > 0
    BET (0, "Bet"),

    //WIN will cash in if winAmount > 0
    WIN (1, "Win"),

    //LOSE will need no actions (no cash in and cash out)
    LOSE (2, "Lose"),

    //JACKPOT will cash in if winAmount > 0
//    JACKPOT (3, "Jackpot"),

    //BET_WIN = combination of BET and WIN, which will create a bet data, and cash out if betAmount > 0 + will cash in if winAmount > 0
    BET_WIN (4, "Bet and Win"),

    //BET_LOSE = combination of BET and LOSE, which will create a bet data, and cash out if betAmount > 0
    BET_LOSE (5, "Bet and Lose"),

    //BET_JACKPOT = combination of BET and JACKPOT, which will create a bet data, and cash out if betAmount > 0 + will cash in if winAmount > 0
    BET_JACKPOT (6, "Bet and Jackpot"),

    //END will need no actions (no cash in and cash out)
    END (99, "End round")
    ;

    public final Integer code;
    public final String description;

    public static final Map<Integer, String> RESULT_TYPE_VALUE = new HashMap<>() {{
        put(0, "BET");
        put(1, "WIN");
        put(2, "LOSE");
        put(3, "JACKPOT");
        put(4, "BET_WIN");
        put(5, "BET_LOSE");
        put(6, "BET_JACKPOT");
        put(99, "END");
    }};


}
