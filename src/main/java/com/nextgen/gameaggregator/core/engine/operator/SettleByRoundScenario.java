package com.nextgen.gameaggregator.core.engine.operator;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

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

    public ResultType getResultType() {
        /**
         * For Settle By Round,
         * the END Signal will be sent a seperate microservice ga-game-round-ended-service
         */
        if (resultType == ResultType.END) {
            return ResultType.LOSE;
        }

        return resultType;
    }
}
