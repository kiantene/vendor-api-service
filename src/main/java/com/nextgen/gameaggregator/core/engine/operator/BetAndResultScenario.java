package com.nextgen.gameaggregator.core.engine.operator;

import com.nextgen.gameaggregator.operator.enums.ResultType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Concrete implementation of OperatorScenario representing a combined “Bet and Result” operation.
 *
 * <p>
 * This scenario is used when both placing a bet and processing its result occur in a single transaction.
 * Unlike per-bet or per-round scenarios, this may combine side effects that would normally be separate.
 * </p>
 *
 * <p>
 * Key behaviors:
 * - Contains the resultType to indicate the outcome (WIN, LOSE, BET_WIN, BET_LOSE, END).
 * - Any Operator Failures should not be Retried
 * </p>
 *
 */
@Getter
@RequiredArgsConstructor
public final class BetAndResultScenario implements OperatorScenario {

    private final ResultType resultType;

}
