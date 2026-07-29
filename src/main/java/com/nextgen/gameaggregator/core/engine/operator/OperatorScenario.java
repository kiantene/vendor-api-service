package com.nextgen.gameaggregator.core.engine.operator;

/**
 * Represents a scenario for an operator request execution.
 *
 * <p>
 * This sealed interface defines the type of operation being performed, such as:
 * - BetScenario: placing a bet
 * - ResultByBetScenario: settling results by bet
 * - ResultByRoundScenario: settling results by round
 * - BetAndResultScenario: combined bet and result operations
 * </p>
 *
 * <p>
 * Key points:
 * - Sealed interface ensures that only the permitted scenarios can implement this interface,
 *   giving compile-time safety and clearer domain modeling.
 * - The default method `shouldRetry()` provides a simple mechanism for adapters to check
 *   if retry logic should be applied for this scenario.
 * - Currently, the default is `false`; specific scenarios can override it to enable retries.
 * - This interface is primarily used in `OperatorRequestAdapter` and `OperatorApiService` to
 *   drive scenario-specific behavior such as retries or fallback handling.
 * </p>
 */
public sealed interface OperatorScenario
        permits BetScenario, SettleByBetScenario, SettleByRoundScenario, BetAndResultScenario {

    /**
     * Determines whether API calls for this scenario should be retried in case of failure.
     *
     * <p>
     * Default behavior is to not retry. Concrete scenario implementations can override this
     * to enable retry logic (e.g., for idempotent operations like settling results).
     * </p>
     *
     * @return true if this scenario allows retries; false otherwise
     */
    default boolean shouldRetry() {
        return false;
    }
}
