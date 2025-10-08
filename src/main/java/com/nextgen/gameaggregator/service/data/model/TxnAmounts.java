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
        super(null, denominationRate);
        this.bet = bet;
        this.win = win;
        this.jackpot = jackpot;
        this.turnover = bet;
        this.winLoss = safeSubtract(win, bet);
    }

    public static TxnAmounts of(GameTransaction txn, BigDecimal fromVendorRate) {
        return new TxnAmounts(
                txn.getBetAmount(),
                txn.getWinAmount(),
                txn.getJackpotAmount(),
                fromVendorRate
        );
    }

    public static TxnAmounts of(GameRound round, BigDecimal fromVendorRate) {
        return new TxnAmounts(
                round.getBetAmount(),
                round.getWinAmount(),
                round.getJackpotAmount(),
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
