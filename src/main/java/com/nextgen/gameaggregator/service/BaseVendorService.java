package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetInformation;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;

public abstract class BaseVendorService {
    public BigDecimal calculateWinLoss(BetInformation betInfo) {
        BigDecimal betAmount = betInfo.getBetAmount();
        BigDecimal winAmount = ObjectUtils.isEmpty(betInfo.getWinAmount())?BigDecimal.valueOf(0):betInfo.getWinAmount();

        return winAmount.subtract(betAmount);
    }

    public BigDecimal calculateEffectiveTurnover(BetInformation betInfo) {
        return betInfo.getBetAmount();
    }

    public BigDecimal calculateWinAmount(BetInformation betInfo) {
        BigDecimal winAmount = ObjectUtils.isEmpty(betInfo.getWinAmount())?BigDecimal.valueOf(0):betInfo.getWinAmount();
        return winAmount;
    }

    public BigDecimal calculateJackpotAmount(BetInformation betInfo) {
        BigDecimal jackpotAmount = ObjectUtils.isEmpty(betInfo.getJackpotAmount())?BigDecimal.valueOf(0):betInfo.getJackpotAmount();
        return jackpotAmount;
    }
}
