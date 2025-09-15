package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackDecisionType;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;

public class RollbackPolicy {

    private RollbackPolicy() {
    }

    public static RollbackDecision decide(GameTransaction betTxn, BetRollbackConfig config) {
        if (betTxn.isRefunded()) {
            return new RollbackDecision(RollbackDecisionType.NO_OP, "Already refunded");
        }
        if (betTxn.isSettled() && !config.isAllowRollbackForSettledBet()) {
            return new RollbackDecision(RollbackDecisionType.REJECT, "Transaction already settled");
        }
        if (betTxn.isUnsettled() && betTxn.isSuccess()) {
            return new RollbackDecision(RollbackDecisionType.ALLOW, "Eligible for rollback");
        }
        // default fallback
        return new RollbackDecision(RollbackDecisionType.REJECT, "Not eligible for rollback");
    }

    public static RollbackDecision decide(GameRound round, BetRollbackConfig config) {
        if (round.isRefunded()) {
            return new RollbackDecision(RollbackDecisionType.NO_OP, "Already refunded");
        }
        if (round.isSettled() && !config.isAllowRollbackForSettledBet()) {
            return new RollbackDecision(RollbackDecisionType.REJECT, "Transaction already settled");
        }
        if (round.isUnsettled()) {
            return new RollbackDecision(RollbackDecisionType.ALLOW, "Eligible for rollback");
        }
        // default fallback
        return new RollbackDecision(RollbackDecisionType.REJECT, "Not eligible for rollback");
    }
}
