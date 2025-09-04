package com.nextgen.gameaggregator.service.business.maxpayout;

import com.nextgen.gameaggregator.entity.ga.BetInformation;

import java.math.BigDecimal;

public record PayoutCapResult(
        BetInformation cappedBetResult,
        BigDecimal uncapWinAmount,
        BigDecimal uncapWinLoss,
        BigDecimal uncapJackpotAmount,
        BigDecimal uncapEffectiveTurnover
) {}
