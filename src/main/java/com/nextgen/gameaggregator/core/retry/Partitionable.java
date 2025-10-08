package com.nextgen.gameaggregator.core.retry;

public interface Partitionable {
    String getPartitionKey();
    Long getTransactionTime();
}
