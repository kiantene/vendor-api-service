package com.nextgen.gameaggregator.operator.wallet.win;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.enums.WinType;

import java.math.BigDecimal;

public interface WinData {
    String getExternalTransactionId();
    BigDecimal getAmount();
    String getRoundId();
    String getGameId();
    Long getTimestamp();
    WinType getWinType();
    BigDecimal getEffectiveTurnover();
    BetResultLog prepareData(BetHistory betHistory, BetResultLog betResultLog);
}
