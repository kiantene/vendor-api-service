package com.nextgen.gameaggregator.core.engine.wallet.bet;

import java.util.Optional;

import com.nextgen.gameaggregator.core.exception.MultipleBetNotAllowedException;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;

public class BetPolicy {
    private BetPolicy() {
    }

    /**
     * Decides whether to allow, reject, or no-op a bet request
     * 
     * @param roundOpt Optional GameRound
     * @param config BetConfig containing bet rules
     * @return BetDecision with the decision type and bet type
     */
    public static BetDecision decide(Optional<GameRound> roundOpt, BetConfig config) {
        boolean isMultipleBet = isMultipleBet(roundOpt);

        // TODO: Need further discussion on handling already ended rounds for bets (AviatorStudio)
        // Reject if round exists and is already ended
        // if (roundOpt.isPresent() && roundOpt.get().isEnded()) {
        //     GameRound round = roundOpt.get();
        //     return BetDecision.reject(
        //             round.getId() + " already ended",
        //             RoundAlreadyEndedException.class
        //     );
        // }

        // Reject if this is a multiple bet and multiple bets are not allowed
        if (isMultipleBet && !config.isAllowMultipleBet()) {
            return BetDecision.reject(
                    "Multiple bets are not allowed for this game",
                    MultipleBetNotAllowedException.class
            );
        }

        return BetDecision.allow();
    }

    /**
     * Determines if the current bet is a multiple bet
     * A bet is considered "multiple" if:
     * - Round exists AND has at least 1 successful bet transaction
     * 
     * @param roundOpt Optional GameRound
     * @return true if this is a multiple bet scenario
     */
    private static boolean isMultipleBet(Optional<GameRound> roundOpt) {
        return roundOpt
                .map(GameRound::hasMultipleBets)
                .orElse(false);
    }
}
