package com.nextgen.gameaggregator.core.engine.operator;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Concrete implementation of OperatorScenario for settling results on a per-round basis.
 *
 * <p>
 * This scenario encapsulates all the context needed to process a single bet settlement:
 * - resultType: outcome of the bet (WIN, LOSE, BET_WIN, BET_LOSE, END)
 * - betTxn: the First Successful Bet GameTransaction
 * </p>
 *
 * <p>
 * Key behaviors:
 * - Any Operator Failures should be added to the Retry Queue to be retired async
 * </p>
 *
 */
@RequiredArgsConstructor
public final class SettleByRoundScenario implements OperatorScenario {

    private final ResultType resultType;
    @Getter
    private final GameTransaction betTxn;

    @Override
    public boolean shouldRetry() {
        return true;
    }

    public ResultType getResultType(GameRound round) {
        if (resultType == ResultType.END) {
            /**
             * Need Check Previous Accumulated Round Result for the Case where the last call is purely for Ending the Round
             */
            BigDecimal winAmount = Optional.ofNullable(round.getWinAmount()).orElse(BigDecimal.ZERO);
            BigDecimal jackpotAmount = Optional.ofNullable(round.getJackpotAmount()).orElse(BigDecimal.ZERO);
            boolean hasWin = winAmount.compareTo(BigDecimal.ZERO) > 0;
            boolean hasJackpot = jackpotAmount.compareTo(BigDecimal.ZERO) > 0;

            return (hasWin || hasJackpot) ? ResultType.WIN : ResultType.END;
        }

        return resultType;
    }
}
