package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackMeta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers GA-14599: {@link BetRollbackProcessor#buildRollbackMeta} must produce the operator-POV
 * per-bet reversal amounts (stake / win / jackpot) the transfer wallet used to read from
 * settled_bet / unsettled_bet. Aggregation is scoped to a single gaBetId.
 */
class BetRollbackProcessorRollbackMetaTest {

    private static final String ROUND_ID = "round-123";
    private static final String BET_A = "ga-bet-A";
    private static final String BET_B = "ga-bet-B";
    /** Identity rate: keeps aggregation-focused tests asserting the raw summed amounts. */
    private static final BigDecimal RATE_ONE = BigDecimal.ONE;

    // --- fixtures -----------------------------------------------------------

    private static RoundTxn txn(TxnType type, TxnStatus status, String gaBetId,
                                String bet, String win, String jackpot) {
        RoundTxn t = new RoundTxn();
        t.setType(type);
        t.setStatus(status);
        t.setGaBetId(gaBetId);
        if (bet != null) t.setBetAmount(new BigDecimal(bet));
        if (win != null) t.setWinAmount(new BigDecimal(win));
        if (jackpot != null) t.setJackpotAmount(new BigDecimal(jackpot));
        return t;
    }

    private static GameRound round(List<RoundTxn> txns) {
        GameRound round = GameRound.of("VendorX", "user1", ROUND_ID);
        round.setTransactions(txns);
        return round;
    }

    private static void assertAmounts(RollbackMeta meta, String betAmount, String winAmount) {
        assertNotNull(meta);
        assertEquals(0, meta.getBetAmount().compareTo(new BigDecimal(betAmount)),
                "betAmount, was: " + meta.getBetAmount());
        assertEquals(0, meta.getWinAmount().compareTo(new BigDecimal(winAmount)),
                "winAmount, was: " + meta.getWinAmount());
    }

    // --- scenarios ----------------------------------------------------------

    @Test
    void unsettledBet_onlyBet_returnsStakeOnly() {
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null)));
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, RATE_ONE), "10", "0");
    }

    @Test
    void settledBet_betPlusResult_foldsJackpotIntoWinAmount() {
        // RESULT win 25 + jackpot 5 => winAmount 30
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null),
                txn(TxnType.RESULT, TxnStatus.SUCCESS, BET_A, null, "25", "5")));
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, RATE_ONE), "10", "30");
    }

    @Test
    void cappedWin_rollbackMetaUsesCappedNotVendorWin() {
        // OVI-2391: RESULT vendor win 25 but capped to 15 (agent max-payout); jackpot 5 uncapped.
        // RollbackMeta is operator-POV (what was posted to the wallet) => capped 15 + jackpot 5 = 20,
        // not the raw vendor 25 + 5 = 30. Without the capped-aware read this would return 30.
        RoundTxn bet = txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null);
        RoundTxn result = txn(TxnType.RESULT, TxnStatus.SUCCESS, BET_A, null, "25", "5");
        result.setCappedWinAmount(new BigDecimal("15"));
        GameRound round = round(List.of(bet, result));
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, RATE_ONE), "10", "20");
    }

    @Test
    void betNResult_singleTxn_foldsJackpotIntoWinAmount() {
        // win 30 + jackpot 7 => winAmount 37
        GameRound round = round(List.of(
                txn(TxnType.BET_N_RESULT, TxnStatus.SUCCESS, BET_A, "10", "30", "7")));
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, RATE_ONE), "10", "37");
    }

    @Test
    void byRound_eachBetGetsItsOwnMeta_noneDropped() {
        // Rollback-by-round issues one rollback call per gaBetId (every bet is rolled back).
        // Each call's meta must carry ONLY that bet's amounts — not the round total and not
        // another bet's amounts. Here both A and B are rolled back, each with its own figures.
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null),
                txn(TxnType.RESULT, TxnStatus.SUCCESS, BET_A, null, "20", null),
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_B, "99", null, null),
                txn(TxnType.RESULT, TxnStatus.SUCCESS, BET_B, null, "88", null)));
        // per-bet call for A
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, RATE_ONE), "10", "20");
        // per-bet call for B (separately rolled back in the same round)
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_B, null, RATE_ONE), "99", "88");
    }

    @Test
    void refundedBet_stakeStillReported() {
        // Already-refunded bet: still report the amount; the transfer wallet's idempotency decides
        // whether to reverse again. GA does not suppress it.
        RoundTxn refundedBet = txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null);
        refundedBet.setState(GameRoundState.REFUNDED);
        GameRound round = round(List.of(refundedBet));
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, RATE_ONE), "10", "0");
    }

    @Test
    void nonSuccessTxn_stillReported_letTransferWalletReconcile() {
        // GA's ERROR/SENT status is GA's own view — the transfer wallet may have actually moved the
        // money on an ambiguous timeout. We always report the amount and let it reconcile/decide,
        // rather than suppress it and risk leaving the player short.
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.ERROR, BET_A, "10", null, null),
                txn(TxnType.RESULT, TxnStatus.SENT, BET_A, null, "20", null)));
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, RATE_ONE), "10", "20");
    }

    @Test
    void nullAmounts_allZeroAfterScan_returnsNull() {
        // Matched txns but every amount is null (a data anomaly — a real bet always has a stake).
        // Both aggregates stay at zero, so no meta is sent rather than a zero-amount reversal.
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, null, null, null),
                txn(TxnType.RESULT, TxnStatus.SUCCESS, BET_A, null, null, null)));
        assertNull(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, RATE_ONE));
    }

    @Test
    void unrelatedTxnTypes_ignored() {
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null),
                txn(TxnType.ROLLBACK, TxnStatus.SUCCESS, BET_A, "999", "999", "999")));
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, RATE_ONE), "10", "0");
    }

    @Test
    void noMatchingGaBetId_returnsNull() {
        // No RoundTxn carries the requested gaBetId -> no amount info -> no meta (fail-safe), not {0,0}.
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_B, "10", null, null)));
        assertNull(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, RATE_ONE));
    }

    @Test
    void nullTransactions_returnsNull() {
        GameRound round = round(null);
        assertNull(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, RATE_ONE));
    }

    // --- operator-POV conversion (fromVendorRate must be applied) ------------

    @Test
    void appliesFromVendorRate_soAmountsAreOperatorPov_notVendorPov() {
        // Vendor amounts bet=10, win=20 at rate 2 => operator-POV betAmount=20, winAmount=40.
        // Distinct from the vendor values (10/20), so a missing conversion would fail this test.
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null),
                txn(TxnType.RESULT, TxnStatus.SUCCESS, BET_A, null, "20", null)));
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, new BigDecimal("2")), "20", "40");
    }

    @Test
    void appliesFromVendorRate_settledWithJackpot() {
        // Vendor bet=10, win=25, jackpot=5 (winAmount vendor=30) at rate 2 => betAmount=20, winAmount=60.
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null),
                txn(TxnType.RESULT, TxnStatus.SUCCESS, BET_A, null, "25", "5")));
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, new BigDecimal("2")), "20", "60");
    }

    @Test
    void appliesFromVendorRate_fractional() {
        // Vendor bet=100 at rate 0.25 => operator-POV betAmount=25.
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "100", null, null)));
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, new BigDecimal("0.25")), "25", "0");
    }

    @Test
    void nullGaBetId_returnsNullMeta_noNpe() {
        // by-bet path can pass betTxn.getGaBetId() == null; must not NPE on the equals check.
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null)));
        assertNull(BetRollbackProcessor.buildRollbackMeta(round, null, null, RATE_ONE));
    }

    @Test
    void nullFromVendorRate_returnsNullMeta_failSafe() {
        // Without a rate we can't express operator-POV — return no meta rather than guess (avoids
        // both an NPE and silently sending vendor amounts as if they were operator-POV).
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null)));
        assertNull(BetRollbackProcessor.buildRollbackMeta(round, BET_A, null, null));
    }

    // --- unsettled fast-path vs settled scan (allowRollbackForSettledBet) -----

    private static GameTransaction betGameTxn(GameRoundState state, String gaBetId, String bet) {
        GameTransaction t = new GameTransaction();
        t.setType(TxnType.BET);
        t.setState(state);
        t.setGaBetId(gaBetId);
        if (bet != null) t.setBetAmount(new BigDecimal(bet));
        return t;
    }

    @Test
    void unsettledBetTxn_fastPath_stakeOnly_doesNotScanForResult() {
        // The norm: rollback of an UNSETTLED bet reverses the stake only. Even if the round somehow
        // carries a RESULT for the same bet, the fast-path uses the BET txn directly and ignores it.
        GameTransaction bet = betGameTxn(GameRoundState.UNSETTLED, BET_A, "10");
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null),
                txn(TxnType.RESULT, TxnStatus.SUCCESS, BET_A, null, "20", null))); // must be ignored
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, bet, RATE_ONE), "10", "0");
    }

    @Test
    void settledBetTxn_scansRound_includesResultWinAndJackpot() {
        // Settled-bet rollback (opt-in): must pull win/jackpot off the separate RESULT txn.
        GameTransaction bet = betGameTxn(GameRoundState.SETTLED, BET_A, "10");
        GameRound round = round(List.of(
                txn(TxnType.BET, TxnStatus.SUCCESS, BET_A, "10", null, null),
                txn(TxnType.RESULT, TxnStatus.SUCCESS, BET_A, null, "20", "5")));
        assertAmounts(BetRollbackProcessor.buildRollbackMeta(round, BET_A, bet, RATE_ONE), "10", "25");
    }
}
