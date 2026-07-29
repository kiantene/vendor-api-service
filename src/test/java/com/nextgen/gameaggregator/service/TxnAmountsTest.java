package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.data.model.TxnAmounts;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TxnAmountsTest {

    private static final BigDecimal RATE_ONE = BigDecimal.ONE;
    private static final BigDecimal RATE_001 = new BigDecimal("0.01");

    // -------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------

    private GameTransaction txn(BigDecimal bet, BigDecimal win, BigDecimal jackpot) {
        GameTransaction txn = new GameTransaction();
        txn.setBetAmount(bet);
        txn.setWinAmount(win);
        txn.setJackpotAmount(jackpot);
        return txn;
    }

    private GameRound round(BigDecimal bet, BigDecimal win, BigDecimal jackpot) {
        GameRound round = new GameRound();
        round.setTransactions(List.of(
                roundTxn(TxnType.BET_N_RESULT, TxnStatus.SUCCESS, GameRoundState.UNSETTLED, bet, win, jackpot)
        ));
        return round;
    }

    private RoundTxn roundTxn(TxnType type, TxnStatus status, GameRoundState state,
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

    // -------------------------------------------------------
    // Constructor behavior (via factories)
    // -------------------------------------------------------

    @Test
    void shouldCalculateBasicAmounts_withoutDenomination() {
        GameTransaction txn = txn(
                new BigDecimal("100"),
                new BigDecimal("40"),
                new BigDecimal("5")
        );

        TxnAmounts amounts = TxnAmounts.of(txn, RATE_ONE);

        assertEquals(new BigDecimal("100"), amounts.getBet());
        assertEquals(new BigDecimal("40"), amounts.getWin());
        assertEquals(new BigDecimal("5"), amounts.getJackpot());
        assertEquals(new BigDecimal("100"), amounts.getTurnover());
        assertEquals(new BigDecimal("-60"), amounts.getWinLoss());
    }

    @Test
    void shouldApplyDenominationRateToAllFields() {
        GameTransaction txn = txn(
                new BigDecimal("1000"),
                new BigDecimal("400"),
                new BigDecimal("50")
        );

        TxnAmounts amounts = TxnAmounts.of(txn, RATE_001);

        assertEquals(new BigDecimal("10"), amounts.getBet());
        assertEquals(new BigDecimal("4"), amounts.getWin());
        assertEquals(new BigDecimal("0.5"), amounts.getJackpot());
        assertEquals(new BigDecimal("10"), amounts.getTurnover());
        assertEquals(new BigDecimal("-6"), amounts.getWinLoss());
    }

    // -------------------------------------------------------
    // effectiveTurnover — provided vs. null
    // -------------------------------------------------------

    @Test
    void of_transaction_whenEffectiveTurnoverProvided_shouldUseThatInsteadOfBet() {
        GameTransaction txn = txn(new BigDecimal("1000"), new BigDecimal("400"), BigDecimal.ZERO);
        txn.setEffectiveTurnover(new BigDecimal("800"));

        TxnAmounts amounts = TxnAmounts.of(txn, RATE_001);

        assertEquals(new BigDecimal("8"), amounts.getTurnover()); // 800 * 0.01
        assertEquals(new BigDecimal("10"), amounts.getBet());     // 1000 * 0.01 unchanged
    }

    @Test
    void of_transaction_whenEffectiveTurnoverNull_shouldFallBackToBet() {
        GameTransaction txn = txn(new BigDecimal("1000"), new BigDecimal("400"), BigDecimal.ZERO);
        // effectiveTurnover not set → null

        TxnAmounts amounts = TxnAmounts.of(txn, RATE_001);

        assertEquals(new BigDecimal("10"), amounts.getTurnover()); // falls back to bet * rate
    }

    @Test
    void of_round_whenEffectiveTurnoverProvided_shouldUseThatInsteadOfBet() {
        GameRound round = round(new BigDecimal("500"), new BigDecimal("200"), BigDecimal.ZERO);
        round.setEffectiveTurnover(new BigDecimal("300"));

        TxnAmounts amounts = TxnAmounts.of(round, RATE_001);

        assertEquals(new BigDecimal("3"), amounts.getTurnover()); // 300 * 0.01
        assertEquals(new BigDecimal("5"), amounts.getBet());      // 500 * 0.01 unchanged
    }

    @Test
    void of_round_whenEffectiveTurnoverNull_shouldFallBackToBet() {
        GameRound round = round(new BigDecimal("500"), new BigDecimal("200"), BigDecimal.ZERO);
        // effectiveTurnover not set → null

        TxnAmounts amounts = TxnAmounts.of(round, RATE_001);

        assertEquals(new BigDecimal("5"), amounts.getTurnover()); // falls back to bet * rate
    }

    // -------------------------------------------------------
    // of(GameTransaction)
    // -------------------------------------------------------

    @Test
    void of_transaction_shouldUseTransactionValues() {
        GameTransaction txn = txn(
                new BigDecimal("200"),
                new BigDecimal("150"),
                new BigDecimal("20")
        );

        TxnAmounts amounts = TxnAmounts.of(txn, RATE_ONE);

        assertEquals(new BigDecimal("200"), amounts.getBet());
        assertEquals(new BigDecimal("150"), amounts.getWin());
        assertEquals(new BigDecimal("20"), amounts.getJackpot());
        assertEquals(new BigDecimal("-50"), amounts.getWinLoss());
    }

    // -------------------------------------------------------
    // of(GameRound)
    // -------------------------------------------------------

    @Test
    void of_round_shouldUseRoundValues() {
        GameRound round = round(
                new BigDecimal("300"),
                new BigDecimal("100"),
                new BigDecimal("30")
        );

        TxnAmounts amounts = TxnAmounts.of(round, RATE_ONE);

        assertEquals(new BigDecimal("300"), amounts.getBet());
        assertEquals(new BigDecimal("100"), amounts.getWin());
        assertEquals(new BigDecimal("30"), amounts.getJackpot());
        assertEquals(new BigDecimal("-200"), amounts.getWinLoss());
    }

    // -------------------------------------------------------
    // of(betTxn, resultTxn)
    // -------------------------------------------------------

    @Test
    void of_betAndResultTxn_shouldCombineCorrectly() {
        GameTransaction betTxn = txn(
                new BigDecimal("500"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        GameTransaction resultTxn = txn(
                BigDecimal.ZERO,
                new BigDecimal("350"),
                new BigDecimal("25")
        );

        TxnAmounts amounts = TxnAmounts.of(betTxn, resultTxn, RATE_ONE);

        assertEquals(new BigDecimal("500"), amounts.getBet());
        assertEquals(new BigDecimal("350"), amounts.getWin());
        assertEquals(new BigDecimal("25"), amounts.getJackpot());
        assertEquals(new BigDecimal("-150"), amounts.getWinLoss());
    }

    // -------------------------------------------------------
    // of(round, resultTxn)  (MOST IMPORTANT)
    // -------------------------------------------------------

    @Test
    void of_roundAndResultTxn_shouldAggregateWinAndJackpot() {
        GameRound round = round(
                new BigDecimal("100"),
                new BigDecimal("50"),
                new BigDecimal("10")
        );

        GameTransaction resultTxn = txn(
                BigDecimal.ZERO,
                new BigDecimal("20"),
                new BigDecimal("5")
        );

        TxnAmounts amounts = TxnAmounts.of(round, resultTxn, RATE_ONE);

        assertEquals(new BigDecimal("100"), amounts.getBet());
        assertEquals(new BigDecimal("70"), amounts.getWin());      // 50 + 20
        assertEquals(new BigDecimal("15"), amounts.getJackpot());  // 10 + 5
        assertEquals(new BigDecimal("-30"), amounts.getWinLoss());
    }

    // -------------------------------------------------------
    // Regression: jackpot duplication bug
    // -------------------------------------------------------

    @Test
    void of_roundAndResultTxn_shouldNotDuplicateRoundJackpot_regression() {
        GameRound round = round(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("20")
        );

        GameTransaction resultTxn = txn(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("3")
        );

        TxnAmounts amounts = TxnAmounts.of(round, resultTxn, RATE_ONE);

        // Correct = 23
        // Buggy implementation would produce 40
        assertEquals(new BigDecimal("23"), amounts.getJackpot());
    }

    // -------------------------------------------------------
    // Null handling
    // -------------------------------------------------------

    @Test
    void of_roundAndResultTxn_shouldHandleNullResultJackpot() {
        GameRound round = round(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("10")
        );

        GameTransaction resultTxn = txn(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );

        TxnAmounts amounts = TxnAmounts.of(round, resultTxn, RATE_ONE);

        assertEquals(new BigDecimal("10"), amounts.getJackpot());
    }

    // -------------------------------------------------------
    // Denomination with combined sources
    // -------------------------------------------------------

    // -------------------------------------------------------
    // ofCapped — agent max-payout (reads capped, coalesces to vendor)
    // -------------------------------------------------------

    @Test
    void ofCapped_transaction_usesCappedWinWhenSet() {
        GameTransaction txn = txn(new BigDecimal("2167"), new BigDecimal("2100"), BigDecimal.ZERO);
        txn.setCappedWinAmount(new BigDecimal("2000"));

        TxnAmounts amounts = TxnAmounts.ofCapped(txn, RATE_ONE);

        assertEquals(new BigDecimal("2000"), amounts.getWin());
        assertEquals(new BigDecimal("-167"), amounts.getWinLoss());
        // plain of() still reads the uncapped vendor win
        assertEquals(new BigDecimal("2100"), TxnAmounts.of(txn, RATE_ONE).getWin());
    }

    @Test
    void ofCapped_transaction_coalescesToVendorWhenUncapped() {
        GameTransaction txn = txn(new BigDecimal("100"), new BigDecimal("40"), new BigDecimal("5"));
        // no cappedWinAmount/cappedJackpotAmount set

        TxnAmounts amounts = TxnAmounts.ofCapped(txn, RATE_ONE);

        assertEquals(new BigDecimal("40"), amounts.getWin());
        assertEquals(new BigDecimal("5"), amounts.getJackpot());
    }

    @Test
    void ofCapped_betAndResultTxn_usesCappedResultWin() {
        GameTransaction betTxn = txn(new BigDecimal("2167"), BigDecimal.ZERO, BigDecimal.ZERO);
        GameTransaction resultTxn = txn(BigDecimal.ZERO, new BigDecimal("2100"), BigDecimal.ZERO);
        resultTxn.setCappedWinAmount(new BigDecimal("2000"));

        TxnAmounts amounts = TxnAmounts.ofCapped(betTxn, resultTxn, RATE_ONE);

        assertEquals(new BigDecimal("2167"), amounts.getBet());
        assertEquals(new BigDecimal("2000"), amounts.getWin());
        assertEquals(new BigDecimal("-167"), amounts.getWinLoss());
    }

    @Test
    void ofCapped_capsJackpotIndependently() {
        GameTransaction txn = txn(new BigDecimal("100"), new BigDecimal("40"), new BigDecimal("3000"));
        txn.setCappedJackpotAmount(new BigDecimal("2000"));

        TxnAmounts amounts = TxnAmounts.ofCapped(txn, RATE_ONE);

        assertEquals(new BigDecimal("40"), amounts.getWin());       // coalesces (no cappedWin)
        assertEquals(new BigDecimal("2000"), amounts.getJackpot()); // capped
    }

    @Test
    void of_roundAndResultTxn_shouldApplyDenominationAfterAggregation() {
        GameRound round = round(
                new BigDecimal("1000"),
                new BigDecimal("200"),
                new BigDecimal("100")
        );

        GameTransaction resultTxn = txn(
                BigDecimal.ZERO,
                new BigDecimal("50"),
                new BigDecimal("25")
        );

        TxnAmounts amounts = TxnAmounts.of(round, resultTxn, RATE_001);

        // (100 + 25) * 0.01 = 1.25
        assertEquals(new BigDecimal("1.25"), amounts.getJackpot());
        assertEquals(new BigDecimal("2.5"), amounts.getWin()); // (200+50)*0.01
        assertEquals(new BigDecimal("10"), amounts.getBet());
    }
}
