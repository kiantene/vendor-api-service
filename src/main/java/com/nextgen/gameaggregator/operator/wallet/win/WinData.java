package com.nextgen.gameaggregator.operator.wallet.win;

import java.math.BigDecimal;

public interface WinData {
    String getExternalTransactionId();
    BigDecimal getAmount();
    String getRoundId();
    String getGameId();
    Long getTimestamp();
    String getWinType();
}
