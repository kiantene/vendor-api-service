package com.nextgen.gameaggregator.service.data.producer;

import com.nextgen.gameaggregator.core.engine.game.round.GameRoundEndedEvent;

public record GameRoundEndedMessage(
        String roundId,
        Long vendorPlayerId,
        Integer vendorGameId,
        String gameCode,
        String operatorUsername,
        String currency,
        String settleType,
        Long eventTime
) {
    public static GameRoundEndedMessage ofEvent(GameRoundEndedEvent event, String settleType) {
        return new GameRoundEndedMessage(
                event.roundId(),
                event.vendorPlayerId(),
                event.vendorGameId(),
                event.gameCode(),
                event.agentPlayerUsername(),
                event.currencyCode(),
                settleType,
                event.eventTime()
        );
    }
}
