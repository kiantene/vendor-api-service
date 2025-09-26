package com.nextgen.gameaggregator.core.retry.enums;

public enum RetryJobStatus {
    PENDING,        // Waiting for job to pick up
    PROCESSING,     // Scheduler has picked up and in processing
    SUCCESS,        // Client side has received the retries and processed successfully
}
