package com.nextgen.gameaggregator.operator.sport.adjustment;

import java.math.BigDecimal;

public interface SportAdjustmentData {
    String getVendorUsername();
    String getVendorBetId();
    String getRoundId();
    String getExternalTransactionId();
    BigDecimal getAmount();
    Long getTimestamp();
}
