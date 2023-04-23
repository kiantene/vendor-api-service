package com.nextgen.gameaggregator.operator.wallet.settled;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;

import java.math.BigDecimal;

public interface SettledData {
    String getExternalTransactionId();
    BigDecimal getAmount();
    String getRoundId();
    String getGameId();
    Long getTimestamp();
    ResultType getWinType();
    BigDecimal getEffectiveTurnover();
    SettledBet prepareData(UnsettledBet unsettledBet, UnsettledBetResult unsettledBetResult, SettledBet settledBet);
}
