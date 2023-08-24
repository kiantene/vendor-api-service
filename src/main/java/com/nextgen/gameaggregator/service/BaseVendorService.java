package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.entity.SettledBet;
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

        BigDecimal effectiveTurnover = betInfo.getEffectiveTurnover();

        //if in the end betData still have null/0 effectiveTurnover, will be using betAmount as effectiveTurnover
        if (effectiveTurnover == null || effectiveTurnover.compareTo(BigDecimal.ZERO) == 0) {
            effectiveTurnover = betInfo.getBetAmount();
        }
        
        return effectiveTurnover;
    }

    //calculate ResultType for sending to operator
    public ResultType calculateResultType(BigDecimal betAmount, BigDecimal winAmount, BigDecimal jackpotAmount, boolean isBet) {

        winAmount = Optional.ofNullable(winAmount).orElse(BigDecimal.ZERO);
        jackpotAmount = Optional.ofNullable(jackpotAmount).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean isJackpotAmountMoreThanZero = jackpotAmount.compareTo(BigDecimal.ZERO) > 0;

        ResultType resultType = (isBet) ? ResultType.BET_LOSE : ResultType.END;

        if (isWinAmountMoreThanZero || isJackpotAmountMoreThanZero) {
            resultType = (isBet) ? ResultType.BET_WIN : ResultType.WIN;
        }

        return resultType;
    }

    public boolean shouldRejectCancelRequest() {
        //Temporary only BGAMING, SpadeGaming, EvoNetent need to accept cancel request
        return true;
    }

    public SettledBet updateSettleBetDataBeforeInsertToKafka(SettledBet settledBet, String rawData) {

        return settledBet;
    }
}
