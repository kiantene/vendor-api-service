package com.nextgen.gameaggregator.operator.sport.refund;

public interface SportRefundData {
    String getExternalTransactionId();

    String getVendorBetId();

    String getRoundId();

    String getVendorPlayerUsername();

    Long getTimestamp();
}
