package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.exception.RoundAlreadyEndedException;
import com.nextgen.gameaggregator.core.exception.RoundNotFoundException;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;

import java.util.Optional;

public class BetResultPolicy {
    private BetResultPolicy() {
    }

    public static BetResultDecision decide(Optional<GameRound> roundOpt, BetResultConfig config) {
        // All rejection conditions first

        // Reject if round exists and is already ended
        if (roundOpt.isPresent() && roundOpt.get().isEnded()) {
            GameRound round = roundOpt.get();
            return BetResultDecision.reject(
                    round.getId() + " already ended",
                    RoundAlreadyEndedException.class
            );
        }

        // Reject if round not found and result-before-bet is not allowed
        if (roundOpt.isEmpty() && !config.isAllowResultBeforeBet()) {
            return BetResultDecision.reject("Round not found", RoundNotFoundException.class);
        }

        // All other cases are allowed:
        // - betAndResult requests (when round not ended)
        // - Round exists and not ended
        // - Round not found but result-before-bet is enabled
        return BetResultDecision.allow();
    }
}
