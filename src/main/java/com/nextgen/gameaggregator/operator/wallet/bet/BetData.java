package com.nextgen.gameaggregator.operator.wallet.bet;

import java.math.BigDecimal;

public interface BetData {
    String getExternalTransactionId();
    BigDecimal getAmount();
    String getRoundId();
    String getGameId();
    Long getTimestamp();
}
