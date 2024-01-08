package com.nextgen.gameaggregator.operator.sport.resettle;

import com.nextgen.gameaggregator.enums.BetStatus;

import java.math.BigDecimal;

public interface SportResettleData {
    String getExternalTransactionId();
    String getVendorBetId();
    String getRoundId();
    String getGameId();
    String getVendorPlayerUsername();
    BigDecimal getBetAmount();
    BigDecimal getWinAmount();
    BigDecimal getWinLoss();
    BigDecimal getEffectiveTurnover();
    Long getVendorBetTime();
    Long getResultTime();
    Long getVendorSettleTime();
    BetStatus getBetStatus();
    BigDecimal getNewWinAmount();
    BigDecimal getCreditAmount();
    BigDecimal getDebitAmount();
}
