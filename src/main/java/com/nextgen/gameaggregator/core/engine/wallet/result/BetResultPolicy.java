package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
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
        if (roundOpt.isPresent() && roundOpt.get().isEnded() && config.isSettledByRound()) {
            GameRound round = roundOpt.get();
            return BetResultDecision.reject(
                    round.getId() + " already ended",
                    RoundAlreadyEndedException.class
            );
        }

        // Reject if round not found and result-before-bet is not allowed
        if (roundOpt.isEmpty() && !config.isBetAndResult() && !config.isAllowResultBeforeBet()) {
            return BetResultDecision.reject("Round not found", RoundNotFoundException.class);
        }

        // All other cases are allowed:
        // - betAndResult requests (when round not ended)
        // - Round exists and not ended
        // - Round not found but result-before-bet is enabled
        return BetResultDecision.allow();
    }

    public static BetResultDecision decideResultBeforeBet(GameRound round, BetResultConfig config) {

        // won't have this scenario if it is bet and result endpoint, so just allow
        if (config.isBetAndResult()) return BetResultDecision.allow();

        // if bet txn exists, txn count should be 2 or more, because bet + result = 2 txn
        boolean betTxnExists = round.getTxnCount() > 1;

        if (betTxnExists) { // if bet txn exists, then do nothing and proceed for settlement
            return BetResultDecision.noop();
        }

        // reject if bet not exists and config is disabled
        if (!config.isAllowResultBeforeBet()) {
            return BetResultDecision.reject("Bet not found", BetNotFoundException.class);
        }
        return BetResultDecision.allow();
    }
}
