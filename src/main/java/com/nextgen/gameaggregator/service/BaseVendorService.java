package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.operator.enums.ResultType;

import java.math.BigDecimal;
import java.util.Optional;

public abstract class BaseVendorService {
    public BigDecimal calculateWinLoss(BetInformation betInfo) {
        BigDecimal betAmount = betInfo.getBetAmount();
        BigDecimal winAmount = Optional.ofNullable(betInfo.getWinAmount()).orElse(BigDecimal.ZERO);

        // According to Justin, we will not add jackpotAmount into winloss as game vendor does not include jackpotAmount in GGR calculations
        // BigDecimal jackpotAmount = Optional.ofNullable(betInfo.getJackpotAmount()).orElse(BigDecimal.ZERO);

        return winAmount.subtract(betAmount);
    }

    public BigDecimal calculateEffectiveTurnover(BetInformation betInfo) {
        return betInfo.getBetAmount();
    }

    //calculate ResultType for sending to operator
    public ResultType calculateResultType(BigDecimal betAmount, BigDecimal winAmount, BigDecimal jackpotAmount, Integer isBet) {

        winAmount = Optional.ofNullable(winAmount).orElse(BigDecimal.ZERO);
        jackpotAmount = Optional.ofNullable(jackpotAmount).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean isJackpotAmountMoreThanZero = jackpotAmount.compareTo(BigDecimal.ZERO) > 0;

        ResultType resultType = (isBet == 1) ? ResultType.BET_LOSE : ResultType.END;

        if (isWinAmountMoreThanZero || isJackpotAmountMoreThanZero) {
            resultType = (isBet == 1) ? ResultType.BET_WIN : ResultType.WIN;
        }

        return resultType;
    }
}
