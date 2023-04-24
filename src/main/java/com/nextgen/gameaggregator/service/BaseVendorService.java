package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.enums.BetResultType;
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

    public Integer calculateBetResultType(BetInformation betInfo) {

        int checkWinAmount = betInfo.getWinAmount().compareTo(BigDecimal.ZERO);
        int checkJackpotAmount = betInfo.getJackpotAmount().compareTo(BigDecimal.ZERO);
        Integer betResultType = BetResultType.LOSE.code;

        if (checkWinAmount >= 0 || checkJackpotAmount >= 0){
            betResultType = BetResultType.WIN.code;
        }
        return betResultType;
    }
}
