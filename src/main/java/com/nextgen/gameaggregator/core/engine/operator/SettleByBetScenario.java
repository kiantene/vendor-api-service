package com.nextgen.gameaggregator.core.engine.operator;

import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Concrete implementation of OperatorScenario for settling results on a per-bet basis.
 *
 * <p>
 * This scenario encapsulates all the context needed to process a single bet settlement:
 * - resultType: outcome of the bet (WIN, LOSE, BET_WIN, BET_LOSE, END)
 * - betTxn: the Bet GameTransaction to be Settled
 * </p>
 *
 * <p>
 * Key behaviors:
 * - Any Operator Failures should be added to the Retry Queue to be retired async
 * </p>
 *
 */
@Getter
@RequiredArgsConstructor
public final class SettleByBetScenario implements OperatorScenario {

    private final ResultType resultType;
    private final GameTransaction betTxn;

    @Override
    public boolean shouldRetry() {
        return true;
    }
}
