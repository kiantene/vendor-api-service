package com.nextgen.gameaggregator.operator.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTypeTest {

    /** isWin() must cover the combined BET_WIN, not just the standalone WIN. */
    @Test
    void isWin_trueForWinBearingTypes() {
        assertTrue(ResultType.WIN.isWin());
        assertTrue(ResultType.BET_WIN.isWin());
    }

    @Test
    void isWin_falseForNonWinTypes() {
        assertFalse(ResultType.BET.isWin());
        assertFalse(ResultType.LOSE.isWin());
        assertFalse(ResultType.BET_LOSE.isWin());
        assertFalse(ResultType.END.isWin());
    }
}
