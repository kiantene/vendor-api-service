package com.nextgen.gameaggregator.vendor.vplus.constant;

import lombok.AllArgsConstructor;

import java.util.Set;

@AllArgsConstructor
public enum BetResultType {

    WIN_NORMAL(0),
    WIN_NORMAL_TRIGGER_FREE_SPIN(1),
    WIN_NORMAL_TRIGGER_SPECIAL(2),
    BUY_FREE_SPIN(3),
    BUY_SPECIAL(4),
    BONUS_GAME_SPECIAL(5),
    BONUS(6),
    FREESPIN_BONUS_JACKPOT(7),
    REFUND(101);

    public final int code;

    private static final Set<Integer> SETTLEMENT_CODES = Set.of(
            WIN_NORMAL.code,
            WIN_NORMAL_TRIGGER_FREE_SPIN.code,
            WIN_NORMAL_TRIGGER_SPECIAL.code,
            BUY_FREE_SPIN.code,
            BUY_SPECIAL.code,
            BONUS_GAME_SPECIAL.code,
            BONUS.code,
            FREESPIN_BONUS_JACKPOT.code
    );

    public static boolean requiresSettlement(int code) {
        return SETTLEMENT_CODES.contains(code);
    }
}
