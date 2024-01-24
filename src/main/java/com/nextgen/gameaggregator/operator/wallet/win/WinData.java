package com.nextgen.gameaggregator.operator.wallet.win;

import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.BetResultLog;
import com.nextgen.gameaggregator.operator.enums.ResultType;

import java.math.BigDecimal;

public interface WinData {
    String getExternalTransactionId();
    BigDecimal getAmount();
    String getRoundId();
    String getGameId();
    Long getTimestamp();
    ResultType getWinType();
    BigDecimal getEffectiveTurnover();
    BetResultLog prepareData(BetHistory betHistory, BetResultLog betResultLog);
}
