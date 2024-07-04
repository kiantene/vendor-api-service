package com.nextgen.gameaggregator.operator.sport.unsettle;

public interface SportUnsettleData {
    String getExternalTransactionId();

    String getVendorBetId();
    
    String getRoundId();

    String getVendorPlayerUsername();

    Long getTimestamp();
}
