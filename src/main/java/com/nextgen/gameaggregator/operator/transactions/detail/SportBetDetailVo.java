package com.nextgen.gameaggregator.operator.transactions.detail;

import java.math.BigDecimal;
import java.util.List;

public interface SportBetDetailVo {

    String getBetNumber();

    String getVendorUsername();

    String getReferenceNumber();

    Long getTransactionTime();

    List<MatchDetailData> getMatchDetail();

    String getOdds();

    BigDecimal getStake();

    BigDecimal getWinLoss();

    String getStatus();

    Boolean getIsCashout();

}
