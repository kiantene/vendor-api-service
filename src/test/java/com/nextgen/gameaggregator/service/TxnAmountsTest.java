package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.service.data.model.TxnAmounts;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
        round.setBetAmount(bet);
        round.setWinAmount(win);
        round.setJackpotAmount(jackpot);
        return round;
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

    @Test
    void of_roundAndResultTxn_shouldHandleZeroValues() {
        GameRound round = round(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        GameTransaction resultTxn = txn(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("7")
        );

        TxnAmounts amounts = TxnAmounts.of(round, resultTxn, RATE_ONE);

        assertEquals(new BigDecimal("7"), amounts.getJackpot());
    }

    // -------------------------------------------------------
    // Denomination with combined sources
    // -------------------------------------------------------

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
