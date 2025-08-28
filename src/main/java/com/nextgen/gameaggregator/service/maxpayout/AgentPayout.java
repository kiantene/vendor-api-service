package com.nextgen.gameaggregator.service.maxpayout;

import com.nextgen.gameaggregator.entity.ga.BetInformation;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class AgentPayout {
    private BigDecimal capWinAmount = BigDecimal.ZERO;
    private BigDecimal capEffectiveTurnover = BigDecimal.ZERO;
    private BigDecimal capWinLoss = BigDecimal.ZERO;
    private BigDecimal capJackpotAmount = BigDecimal.ZERO;

    protected AgentPayout(BetInformation betInformation) {
        this.capWinAmount = betInformation.getWinAmount();
        this.capWinLoss = betInformation.getWinLoss();
        this.capJackpotAmount = betInformation.getJackpotAmount();
        this.capEffectiveTurnover = betInformation.getEffectiveTurnover();

    }

    protected AgentPayout(BigDecimal payoutCap, BetInformation betInformation) {

        this.calculateWinAmount(payoutCap, betInformation.getWinAmount());
        this.calculateJackpotAmount(payoutCap, betInformation.getJackpotAmount());
        this.calculateWinLoss(betInformation.getBetAmount());
        this.calculateEffectiveTurnover(betInformation.getEffectiveTurnover());

    }

    private void calculateWinAmount (BigDecimal payoutCap, BigDecimal winAmount) {
        this.capWinAmount = (winAmount.compareTo(payoutCap) > 0) ? payoutCap : winAmount;
    }

    private void calculateJackpotAmount (BigDecimal payoutCap, BigDecimal capJackpotAmount) {
        this.capJackpotAmount = (capJackpotAmount.compareTo(payoutCap) > 0) ? payoutCap : capJackpotAmount;
    }

    private void calculateWinLoss (BigDecimal betAmount) {
        this.capWinLoss = this.capWinAmount.subtract(betAmount);
    }

    private void calculateEffectiveTurnover (BigDecimal effectiveTurnover) {
        this.capEffectiveTurnover = effectiveTurnover;
    }

}
