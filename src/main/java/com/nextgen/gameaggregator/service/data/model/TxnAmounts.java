package com.nextgen.gameaggregator.service.data.model;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;

import java.math.BigDecimal;

public class TxnAmounts extends TxnAmount {
    private final BigDecimal bet;
    private final BigDecimal win;
    private final BigDecimal jackpot;
    private BigDecimal turnover;
    private BigDecimal winLoss;

    public TxnAmounts(BigDecimal bet, BigDecimal win, BigDecimal jackpot, BigDecimal denominationRate) {
        this(bet, win, jackpot, null, denominationRate);
    }

    public TxnAmounts(BigDecimal bet, BigDecimal win, BigDecimal jackpot,
                      BigDecimal effectiveTurnover, BigDecimal denominationRate) {
        super(null, denominationRate);
        this.bet = bet;
        this.win = win;
        this.jackpot = jackpot;
        this.turnover = effectiveTurnover != null ? effectiveTurnover : bet;
        this.winLoss = safeSubtract(win, bet);
    }

    public static TxnAmounts of(GameTransaction txn, BigDecimal fromVendorRate) {
        return new TxnAmounts(
                txn.getBetAmount(),
                txn.getWinAmount(),
                txn.getJackpotAmount(),
                txn.getEffectiveTurnover(),
                fromVendorRate
        );
    }

    public static TxnAmounts of(GameRound round, BigDecimal fromVendorRate) {
        // Round-level totals derived from transactions[*] (see GameRound.computeTotals).
        var totals = round.computeTotals();
        return new TxnAmounts(
                totals.bet(),
                totals.win(),
                totals.jackpot(),
                round.getEffectiveTurnover(),
                fromVendorRate
        );
    }

    public static TxnAmounts of(GameTransaction betTxn, GameTransaction resultTxn, BigDecimal fromVendorRate) {
        return new TxnAmounts(
                betTxn.getBetAmount(),
                resultTxn.getWinAmount(),
                resultTxn.getJackpotAmount(),
                resultTxn.getEffectiveTurnover(),
                fromVendorRate
        );
    }

    public static TxnAmounts of(GameRound round, GameTransaction resultTxn, BigDecimal fromVendorRate) {
        // Round totals + the additional resultTxn (not yet folded into transactions[*]).
        var totals = round.computeTotals();
        BigDecimal extraJackpot = resultTxn.getJackpotAmount() == null ? BigDecimal.ZERO : resultTxn.getJackpotAmount();
        return new TxnAmounts(
                totals.bet(),
                totals.win().add(resultTxn.getWinAmount()),
                totals.jackpot().add(extraJackpot),
                round.getEffectiveTurnover(),
                fromVendorRate
        );
    }

    public BigDecimal getBet() {
        return safeMultiply(bet, denominationRate);  // 1,000,000 × 0.001 = 1,000
    }

    public BigDecimal getWin() {
        return safeMultiply(win, denominationRate);
    }

    public BigDecimal getJackpot() {
        return safeMultiply(jackpot, denominationRate);
    }

    public BigDecimal getTurnover() {
        return safeMultiply(turnover, denominationRate);
    }

    public BigDecimal getWinLoss() {
        return safeMultiply(winLoss, denominationRate);
    }
}
