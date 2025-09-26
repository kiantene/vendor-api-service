package com.nextgen.gameaggregator.core.retry;

public interface Partitionable {
    int calculatePartition(int totalPartitions);
    String getPartitionKey();
    Long getTransactionTime();
}
