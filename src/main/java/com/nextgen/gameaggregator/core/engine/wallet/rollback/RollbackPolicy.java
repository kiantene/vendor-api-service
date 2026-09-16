package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.exception.BetAlreadySettledException;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.core.exception.RoundAlreadyEndedException;
import com.nextgen.gameaggregator.core.exception.RoundAlreadyVoidException;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnStatus;

import java.math.BigDecimal;

public class RollbackPolicy {

    private RollbackPolicy() {
    }

    public static RollbackDecision decide(GameTransaction betTxn, GameRound round, BetRollbackConfig config, BigDecimal rollbackAmount) {
        if (config.getRollbackType() == null) {
            return RollbackDecision.reject(
                    "Invalid configuration state: rollbackType is not explicitly defined.",
                    RollbackNotAllowedException.class
            );
        }

        // Ensure amount validation only executes for rollbackType (BY_BET);
        // aggregate round-scoped rollbacks (BY_ROUND) bypass single-transaction amount matching.
        if (config.isValidateAmountWithBet() && config.isRollbackByBet()) {
            if (rollbackAmount == null) {
                return RollbackDecision.reject("Rollback rejected: rollbackAmount not provided",
                        RollbackNotAllowedException.class);
            }

            if (betTxn.getBetAmount() == null) {
                return RollbackDecision.reject("Rollback rejected: betAmount not available on original bet transaction",
                        RollbackNotAllowedException.class);
            }

            if (rollbackAmount.compareTo(betTxn.getBetAmount()) != 0) {
                return RollbackDecision.reject("Rollback rejected: Amount Mismatch.",
                        RollbackNotAllowedException.class);
            }
        }

        if (betTxn.isRefunded()) {
            return RollbackDecision.noop("Already refunded");
        }

        //Not Allow Rollback When Round Has Result
        if (!config.isAllowRollbackWhenRoundHasResult() && round.hasResultTransaction()) {
            return RollbackDecision.reject("Rollback rejected: round Id = " + round.getRoundId() + " already has a successful result",
                    RollbackNotAllowedException.class);
        }

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
