package com.nextgen.gameaggregator.core.engine.operator;

/**
 * Concrete implementation of OperatorScenario representing a place bet transaction.
 *
 * <p>
 * This scenario is used when only placing a bet is required, without immediately processing the result.
 * It is a minimal scenario class with no additional fields or behavior.
 * </p>
 *
 * <p>
 * Key behaviors:
 * - Any Operator Failures should not be Retried
 * </p>
 *
 */
public final class BetScenario implements OperatorScenario {
}
