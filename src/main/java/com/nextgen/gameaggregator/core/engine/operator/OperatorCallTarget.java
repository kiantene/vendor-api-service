package com.nextgen.gameaggregator.core.engine.operator;

/**
 * Minimal call-target metadata required to dispatch an operator wallet API request.
 *
 * <p>Carries only the four fields the operator-call layer actually consumes:
 * the agent identity (for credential lookup), the player username (for the outbound
 * request envelope), the wallet currency, and the player session token.
 *
 * <p>Sibling of the legacy {@code GameRound + AgentMeta} pair, designed to be a clean
 * input for a future SDK/library extraction with no persistence-layer coupling.
 */
public record OperatorCallTarget(
        Integer agentId,
        String agentPlayerUsername,
        String currency,
        String sessionToken
) {}
