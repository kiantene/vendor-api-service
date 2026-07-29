package com.nextgen.gameaggregator.service.data.producer.endround;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;

public record RoundEndedTriggerMessage(String roundDocId) {
    public static RoundEndedTriggerMessage of(GameRound gameRound) {
        return new RoundEndedTriggerMessage(gameRound.getId());
    }
}
