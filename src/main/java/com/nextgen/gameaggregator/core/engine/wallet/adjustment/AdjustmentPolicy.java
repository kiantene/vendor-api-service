package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import com.nextgen.gameaggregator.core.exception.RoundAlreadyVoidException;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;

public class AdjustmentPolicy {
    private AdjustmentPolicy() {
    }

    public static AdjustmentDecision decide(GameRound round) {
        // All rejection conditions first

        // Reject if round exists and is already void
        if (round.isVoid()) {
            return AdjustmentDecision.reject(
                    round.getId() + " already void",
                    RoundAlreadyVoidException.class
            );
        }

        // All other cases are allowed:
        // - Round exists and not void
        return AdjustmentDecision.allow();
    }
}
