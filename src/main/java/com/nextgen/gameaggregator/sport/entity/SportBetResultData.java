package com.nextgen.gameaggregator.sport.entity;

import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;

import java.math.BigDecimal;

public interface SportBetResultData extends BetResultData {
    String getVendorPlayerUsername();
    BigDecimal getActualBetAmount();
    BigDecimal getOdds();
    Integer getOddTypeId();
}
