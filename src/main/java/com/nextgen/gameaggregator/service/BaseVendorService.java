package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import org.springframework.util.ObjectUtils;

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

    public BigDecimal calculateWinAmount(BetInformation betInfo) {
        return ObjectUtils.isEmpty(betInfo.getWinAmount()) ? BigDecimal.valueOf(0) : betInfo.getWinAmount();
    }

    public BigDecimal calculateJackpotAmount(BetInformation betInfo) {
        return ObjectUtils.isEmpty(betInfo.getJackpotAmount()) ? BigDecimal.valueOf(0) : betInfo.getJackpotAmount();
    }

    public Integer calculateBetResultType(BetInformation betInfo) {
        int checkWinAmount = betInfo.getWinAmount().compareTo(BigDecimal.ZERO);
        int checkJackpotAmount = betInfo.getJackpotAmount().compareTo(BigDecimal.ZERO);
        int checkBetAmount = betInfo.getBetAmount().compareTo(BigDecimal.ZERO);
        Integer betResultType = BetResultType.LOSE.code;

        if (checkBetAmount == 0) {
            betResultType = BetResultType.WIN.code;
        } else if (checkWinAmount > 0 || checkJackpotAmount > 0) {
            betResultType = BetResultType.WIN.code;
        }

        return betResultType;
    }

    //calculate ResultType for sending to operator
    public ResultType calculateResultType(BigDecimal betAmount, BigDecimal winAmount, BigDecimal jackpotAmount, Integer isBet) {

        BigDecimal getWinAmount = ObjectUtils.isEmpty(winAmount) ? BigDecimal.valueOf(0) : winAmount;
        BigDecimal getJackpotAmount = ObjectUtils.isEmpty(jackpotAmount) ? BigDecimal.valueOf(0) : jackpotAmount;

        int checkWinAmount = getWinAmount.compareTo(BigDecimal.ZERO);
        int checkJackpotAmount = getJackpotAmount.compareTo(BigDecimal.ZERO);
        int checkBetAmount = betAmount.compareTo(BigDecimal.ZERO);
        ResultType resultType = (isBet == 1) ? ResultType.BET_LOSE : ResultType.LOSE;

        if (checkBetAmount == 0) {
            resultType = (isBet == 1) ? ResultType.BET_WIN : ResultType.WIN;
        } else if (checkWinAmount > 0 || checkJackpotAmount > 0) {
            resultType = (isBet == 1) ? ResultType.BET_WIN : ResultType.WIN;
        }

        return resultType;
    }
}
