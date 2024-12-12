package com.nextgen.gameaggregator.core;

public interface RequestIdempotency {

    String getTransactionId();
    String getVendorPlayerUsername();
}
