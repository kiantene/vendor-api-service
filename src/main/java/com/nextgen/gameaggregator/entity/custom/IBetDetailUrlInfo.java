package com.nextgen.gameaggregator.entity.custom;

import java.math.BigDecimal;

public interface IBetDetailUrlInfo {

    String getBetId();
    String getTransactionId();
    String getExternalTransactionId();
    String getExternalRoundId();
    String getUsername();
    Integer getCurrencyId();
    String getCurrencyCode();
    String getVendorCurrencyCode();
    String getGameCode();
    Integer getVendorId();
    String getVendorCode();
    String getGameCategoryCode();
    BigDecimal getBetAmount();
    BigDecimal getWinAmount();
    BigDecimal getWinLoss();
    BigDecimal getEffectiveTurnover();
    BigDecimal getJackpotAmount();
    BigDecimal getRefundAmount();
    Integer getStatus();
    Long getVendorBetTime();
    Long getVendorSettleTime();
    Integer getVendorLineId();
    String getIsFreeSpin();
    String getVendorUsername();
}
