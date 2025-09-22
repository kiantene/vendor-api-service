package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.exception.RoundAlreadyEndedException;
import com.nextgen.gameaggregator.core.exception.RoundNotFoundException;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;

import java.util.Optional;

public class BetResultPolicy {
    private BetResultPolicy() {
    }

    public static BetResultDecision decide(Optional<GameRound> roundOpt, BetResultConfig config) {
        /**
         * If this is betAndResult type request, round present or not doesn't matter as it will still be processed
         *
         */
        if (config.isBetAndResult()) {
            return BetResultDecision.allow();
        }

        /**
         * If round is present but ended
         */
        if (roundOpt.isPresent() && roundOpt.get().isEnded()) {
            return BetResultDecision.reject(roundOpt.get().getId() + " already ended", RoundAlreadyEndedException.class);
        }

        /**
         * If round is present but not ended
         * - there is at least 1 unsettled bet in the round to be settled.
         */
        if (roundOpt.isPresent()) {
            return BetResultDecision.allow();
        }

        /**
         * If round not present and the request is not betNResult
         * We will only allow if resultBeforeBet is enabled
         * This is to handle race condition whereby result is received first
         */
        if (!config.isAllowResultBeforeBet()) {
            return BetResultDecision.reject("Round not found", RoundNotFoundException.class);
        }

        return BetResultDecision.allow();
    }
}
