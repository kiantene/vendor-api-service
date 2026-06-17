package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.exception.BetAlreadySettledException;
import com.nextgen.gameaggregator.core.exception.RoundAlreadyEndedException;
import com.nextgen.gameaggregator.core.exception.RoundAlreadyVoidException;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnStatus;

public class RollbackPolicy {

    private RollbackPolicy() {
    }

    public static RollbackDecision decide(GameTransaction betTxn, BetRollbackConfig config) {
        if (betTxn.isUnsettled() && isAllowedStatus(betTxn)) {
            return RollbackDecision.allow();
        }

        if (betTxn.isUnsettled() && isPendingStatus(betTxn)) {
            return RollbackDecision.defer("Defer Rollback as Bet is still Pending");
        }

        if (betTxn.isRefunded()) {
            return RollbackDecision.noop("Already refunded");
        }

        if (betTxn.isSettled() && !config.isAllowRollbackForSettledBet()) {
            return RollbackDecision.reject("Transaction already settled", BetAlreadySettledException.class);
        }

        // default fallback
        return RollbackDecision.allow();
    }

    /**
     * TODO: To Decide If we need a Defer Scenario for RollbackByRound
     */
    public static RollbackDecision decide(GameRound round, BetRollbackConfig config) {
        // Reject if round exists and is already void
        if (round.isVoid()) {
            return RollbackDecision.reject(
                    round.getId() + " already void",
                    RoundAlreadyVoidException.class);
        }

//        if (round.isUnsettled()) {
//            return RollbackDecision.allow();
//        }
//
//        if (round.isRefunded()) {
//            return RollbackDecision.noop("Already refunded");
//        }

        if (round.isEnded() && !config.isAllowRollbackForSettledBet()) {
            return RollbackDecision.reject(round.getId() + " already ended", RoundAlreadyEndedException.class);
        }

        // default fallback
        return RollbackDecision.allow();
    }

    private static boolean isAllowedStatus(GameTransaction betTxn) {
        TxnStatus status = betTxn.getStatus();

        return betTxn.isSuccess() ||
                status == TxnStatus.ERROR ||
                status == TxnStatus.TIMEOUT;
    }

    private static boolean isPendingStatus(GameTransaction betTxn) {
        TxnStatus status = betTxn.getStatus();

        return status == TxnStatus.SENT;
    }
}
