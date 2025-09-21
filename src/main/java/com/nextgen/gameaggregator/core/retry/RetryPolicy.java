package com.nextgen.gameaggregator.core.retry;

import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;

public class RetryPolicy {
    private RetryPolicy() {}

    public static boolean shouldRetry(RetryOrigin origin, Exception ex) {
        return !(ex instanceof InsufficientBalanceException);
    }
}
