package com.nextgen.gameaggregator.sport.entity;

import com.nextgen.gameaggregator.enums.BetStatus;

import java.math.BigDecimal;

public interface SportBetResultData {
    String getExternalTransactionId();
    String getVendorBetId();
    String getRoundId();
    String getGameId();
    String getVendorPlayerUsername();
    BigDecimal getBetAmount();
    BigDecimal getNewBetAmount();
    BigDecimal getWinAmount();
    BigDecimal getWinLoss();
    BigDecimal getEffectiveTurnover();
    Long getVendorBetTime();
    Long getResultTime();
    Long getVendorSettleTime();
    BetStatus getBetStatus();
}
