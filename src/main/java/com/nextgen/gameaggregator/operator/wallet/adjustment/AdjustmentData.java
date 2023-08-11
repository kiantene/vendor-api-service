package com.nextgen.gameaggregator.operator.wallet.adjustment;

import java.math.BigDecimal;

public interface AdjustmentData {
    String getVendorBetId();
    String getRoundId();
    String getExternalTransactionId();
    String getGameId();
    BigDecimal getAdjustmentAmount();
    Long getTimestamp();
}
