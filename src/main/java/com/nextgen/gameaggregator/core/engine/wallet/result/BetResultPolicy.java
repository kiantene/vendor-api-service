package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.enums.BetResultDecisionType;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;

public class BetResultPolicy {
    private BetResultPolicy() {
    }

    public static BetResultDecisionType decide(GameRound round, BetResultConfig config) {
        return BetResultDecisionType.REJECT;
    }
}
