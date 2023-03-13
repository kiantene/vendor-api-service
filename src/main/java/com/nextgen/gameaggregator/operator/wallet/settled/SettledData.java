package com.nextgen.gameaggregator.operator.wallet.settled;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.WinType;

import java.math.BigDecimal;

public interface SettledData {
    String getExternalTransactionId();
    BigDecimal getAmount();
    String getRoundId();
    String getGameId();
    Long getTimestamp();
    WinType getWinType();
    BigDecimal getEffectiveTurnover();
    RawSettledBet prepareData(RawUnsettledBet rawUnsettledBet, RawResultBet rawResultBet, RawSettledBet rawSettledBet);
}
