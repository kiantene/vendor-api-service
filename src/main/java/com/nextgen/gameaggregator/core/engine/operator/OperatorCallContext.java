package com.nextgen.gameaggregator.core.engine.operator;

import com.nextgen.gameaggregator.core.common.OperatorRequestObject;

/**
 * Context object for executing an operator wallet API request from the new
 * debit/credit framework. Sibling of {@link com.nextgen.gameaggregator.core.context.OperatorRequestContext}
 * but built around an {@link OperatorCallTarget} instead of the legacy {@code GameRound}
 * — keeps the operator-call surface decoupled from persistence shapes.
 *
 * <p>Field map relative to {@code OperatorRequestContext}:
 * <ul>
 *   <li>{@code request}, {@code timeoutInMillis}, {@code endpoint} — unchanged.</li>
 *   <li>{@code target} — replaces {@code GameRound round}; carries the agent + currency
 *       + token only.</li>
 *   <li>{@code transaction} — dropped; the new framework persists {@code CreditTxn} /
 *       {@code DebitTxn}, not {@code GameTransaction}.</li>
 *   <li>{@code scenario} — dropped. On the legacy context it drives the result engine's
 *       retry-enqueue; nothing on this surface retries, so nothing read it.</li>
 * </ul>
 *
 * @param <R> operator request payload type
 */
public record OperatorCallContext<R extends OperatorRequestObject>(
        R request,
        int timeoutInMillis,
        String endpoint,
        OperatorCallTarget target
) {}
