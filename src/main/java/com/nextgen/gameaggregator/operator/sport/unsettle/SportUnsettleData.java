package com.nextgen.gameaggregator.operator.sport.unsettle;

public interface SportUnsettleData {
    String getExternalTransactionId();

    String getRoundId();

    String getVendorPlayerUsername();

    Long getTimestamp();
}
