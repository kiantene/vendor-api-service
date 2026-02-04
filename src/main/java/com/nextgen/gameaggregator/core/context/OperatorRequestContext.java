package com.nextgen.gameaggregator.core.context;

import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import com.nextgen.gameaggregator.core.engine.operator.OperatorScenario;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;

/**
 * Context object for executing an operator API request.
 *
 * <p>
 * Encapsulates all the data required to perform an operator API call, including:
 * - request: the operator-specific request payload (DTO)
 * - timeoutInMillis: vendor-specific timeout for the API call in milliseconds
 * - endpoint: operator API endpoint being invoked
 * - round: the current game round
 * - transaction: the current game transaction associated with this request
 * - scenario: the OperatorScenario that defines the type of operation (e.g., bet, result, settle)
 * </p>
 *
 * <p>
 * Key behaviors and design:
 * - Designed to be passed to OperatorApiService and OperatorRequestAdapter for execution and validation.
 * </p>
 *
 * <p>
 * Usage:
 * - Provides a single object for adapters and services to access all information needed for an operator call.
 * - Helps maintain single responsibility by separating data from execution logic.
 * - Ensures consistency when passing round, transaction, and scenario information through the call chain.
 * </p>
 *
 * <p>
 * Considerations / Potential Improvements:
 * - Timeout could be wrapped in a Duration object for clarity and type safety.
 * - Endpoint could be an enum to prevent invalid values.
 * - Optionally provide convenience methods to access traceId, username, or currency from the request or round.
 * </p>
 *
 * @param <R> Operator Request Object, eg: OperatorBetRequest, OperatorResultRequest, etc
 * @param <S> Operator Scenario, eg: BetScenario, SettleByBetScenario, SettleByRoundScenario, BetAndResultScenario, etc
 */
public record OperatorRequestContext<R extends OperatorRequestObject, S extends OperatorScenario> (
        R request,
        int timeoutInMillis,
        String endpoint,
        GameRound round,
        GameTransaction transaction,
        S scenario
) {}
