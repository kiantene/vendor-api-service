package com.nextgen.gameaggregator.core.engine.game.round;

import com.nextgen.gameaggregator.entity.ga.GameSession;

import java.time.Instant;

public record GameRoundEndedEvent(
        String roundId,
        Integer agentId,
        Long vendorPlayerId,
        Integer vendorGameId,
        String gameCode,
        String agentPlayerUsername,
        String currencyCode,
        Long eventTime
) {
    public static GameRoundEndedEvent ofGameSession(GameSession gameSession, String roundId) {
        return new GameRoundEndedEvent(
                roundId,
                gameSession.getAgentId(),
                gameSession.getVendorPlayerId(),
                gameSession.getVendorGameId(),
                gameSession.getGameCode(),
                gameSession.getAgentPlayerUsername(),
                gameSession.getCurrencyCode(),
                Instant.now().toEpochMilli()
        );
    }
}
