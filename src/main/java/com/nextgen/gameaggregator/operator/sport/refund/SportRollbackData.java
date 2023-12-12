package com.nextgen.gameaggregator.operator.sport.refund;

public interface SportRollbackData {
    String getExternalTransactionId();
    Long getVendorSettledTime();
}
