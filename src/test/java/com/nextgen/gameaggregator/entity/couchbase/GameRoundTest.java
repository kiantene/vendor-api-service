package com.nextgen.gameaggregator.entity.couchbase;

import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameRoundTest {

    private static final BigDecimal TEN = new BigDecimal("10");
    private static final BigDecimal FIVE = new BigDecimal("5");
    private static final BigDecimal TWO = new BigDecimal("2");

    @Test
    void computeTotals_returnsZeros_whenNoTransactions() {
        GameRound round = new GameRound();

        GameRound.Totals totals = round.computeTotals();

        assertEquals(BigDecimal.ZERO, totals.bet());
        assertEquals(BigDecimal.ZERO, totals.win());
        assertEquals(BigDecimal.ZERO, totals.jackpot());
    }

    @Test
    void computeTotals_skipsNonSuccessTxns() {
        GameRound round = new GameRound();
        round.setTransactions(List.of(
                txn(TxnType.BET, TxnStatus.ERROR, GameRoundState.UNSETTLED, TEN, null, null),
                txn(TxnType.BET, TxnStatus.SENT,  GameRoundState.UNSETTLED, TEN, null, null)
        ));

        assertEquals(BigDecimal.ZERO, round.computeTotals().bet());
    }

    @Test
    void computeTotals_sumsSuccessfulBetAndResult() {
        GameRound round = new GameRound();
        round.setTransactions(List.of(
                txn(TxnType.BET,    TxnStatus.SUCCESS, GameRoundState.UNSETTLED, TEN, null, null),
                txn(TxnType.RESULT, TxnStatus.SUCCESS, GameRoundState.SETTLED,   null, FIVE, TWO)
        ));

        GameRound.Totals totals = round.computeTotals();

        assertEquals(TEN,  totals.bet());
        assertEquals(FIVE, totals.win());
        assertEquals(TWO,  totals.jackpot());
    }

    @Test
    void computeTotals_betNResult_contributesAllAmounts() {
        GameRound round = new GameRound();
        round.setTransactions(List.of(
                txn(TxnType.BET_N_RESULT, TxnStatus.SUCCESS, GameRoundState.SETTLED, TEN, FIVE, TWO)
        ));

        GameRound.Totals totals = round.computeTotals();

        assertEquals(TEN,  totals.bet());
        assertEquals(FIVE, totals.win());
        assertEquals(TWO,  totals.jackpot());
    }

    /** The core fix: a refunded BET plus a successful ROLLBACK must net to zero. */
    @Test
    void computeTotals_refundedBetAndRollback_netsToZero() {
        GameRound round = new GameRound();
        round.setTransactions(List.of(
                txn(TxnType.BET,      TxnStatus.SUCCESS, GameRoundState.REFUNDED,  TEN, null, null),
                txn(TxnType.ROLLBACK, TxnStatus.SUCCESS, GameRoundState.COMPLETED, TEN, null, null)
        ));

        GameRound.Totals totals = round.computeTotals();

        assertEquals(BigDecimal.ZERO, totals.bet());
        assertEquals(BigDecimal.ZERO, totals.win());
        assertEquals(BigDecimal.ZERO, totals.jackpot());
    }

    @Test
    void computeTotals_walletOpsAreIgnored() {
        GameRound round = new GameRound();
        round.setTransactions(List.of(
                txn(TxnType.DEBIT,  TxnStatus.SUCCESS, GameRoundState.SETTLED, TEN, TEN, TEN),
                txn(TxnType.CREDIT, TxnStatus.SUCCESS, GameRoundState.SETTLED, TEN, TEN, TEN),
                txn(TxnType.PAYOUT, TxnStatus.SUCCESS, GameRoundState.SETTLED, TEN, TEN, TEN)
        ));

        GameRound.Totals totals = round.computeTotals();

        assertEquals(BigDecimal.ZERO, totals.bet());
        assertEquals(BigDecimal.ZERO, totals.win());
        assertEquals(BigDecimal.ZERO, totals.jackpot());
    }

    // ---- computeCappedTotals (agent max-payout) ----

    /** OVI-2391: capped total uses the per-slot capped win, not the raw vendor win. */
    @Test
    void computeCappedTotals_usesCappedWin_whenSlotIsCapped() {
        BigDecimal bet = new BigDecimal("2167");
        BigDecimal vendorWin = new BigDecimal("2100");
        BigDecimal cappedWin = new BigDecimal("2000");

        RoundTxn betTxn = txn(TxnType.BET, TxnStatus.SUCCESS, GameRoundState.UNSETTLED, bet, null, null);
        RoundTxn resultTxn = txn(TxnType.RESULT, TxnStatus.SUCCESS, GameRoundState.SETTLED, null, vendorWin, null);
        resultTxn.setCappedWinAmount(cappedWin);

        GameRound round = new GameRound();
        round.setTransactions(List.of(betTxn, resultTxn));

        // capped total = capped win; vendor total stays uncapped
        assertEquals(cappedWin, round.computeCappedTotals().win());
        assertEquals(vendorWin, round.computeTotals().win());
        // win_loss derived downstream: capped -167 vs uncapped -67
        assertEquals(new BigDecimal("-167"), round.computeCappedTotals().win().subtract(round.computeCappedTotals().bet()));
        assertEquals(new BigDecimal("-67"),  round.computeTotals().win().subtract(round.computeTotals().bet()));
    }

    /** No cap recorded on the slot → capped total coalesces to the vendor amount. */
    @Test
    void computeCappedTotals_coalescesToVendor_whenSlotUncapped() {
        GameRound round = new GameRound();
        round.setTransactions(List.of(
                txn(TxnType.BET,    TxnStatus.SUCCESS, GameRoundState.UNSETTLED, TEN, null, null),
                txn(TxnType.RESULT, TxnStatus.SUCCESS, GameRoundState.SETTLED,   null, FIVE, TWO)
        ));

        assertEquals(FIVE, round.computeCappedTotals().win());
        assertEquals(TWO,  round.computeCappedTotals().jackpot());
    }

    /** Jackpot is capped independently of win. */
    @Test
    void computeCappedTotals_capsJackpotIndependently() {
        RoundTxn resultTxn = txn(TxnType.RESULT, TxnStatus.SUCCESS, GameRoundState.SETTLED, null, FIVE, TEN);
        resultTxn.setCappedJackpotAmount(TWO);

        GameRound round = new GameRound();
        round.setTransactions(List.of(resultTxn));

        assertEquals(FIVE, round.computeCappedTotals().win());     // win untouched (no cappedWin set → coalesce)
        assertEquals(TWO,  round.computeCappedTotals().jackpot()); // jackpot capped
    }

    private static RoundTxn txn(TxnType type, TxnStatus status, GameRoundState state,
                                BigDecimal bet, BigDecimal win, BigDecimal jackpot) {
        RoundTxn t = new RoundTxn();
        t.setType(type);
        t.setStatus(status);
        t.setState(state);
        t.setBetAmount(bet);
        t.setWinAmount(win);
        t.setJackpotAmount(jackpot);
        return t;
    }
}
