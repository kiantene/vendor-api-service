package com.nextgen.gameaggregator.operator.sport.refund;

public interface SportRefundData {
    String getExternalTransactionId();

    String getRoundId();

    String getVendorPlayerUsername();

    Long getTimestamp();
}
