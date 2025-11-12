package com.nextgen.gameaggregator.core.retry;

import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.core.retry.enums.RetryOrigin;
import com.nextgen.gameaggregator.core.webclient.exception.ClientApiResponseParseException;

public class RetryPolicy {
    private RetryPolicy() {}

    public static boolean shouldRetry(RetryOrigin origin, Exception ex) {
        return !(ex instanceof InsufficientBalanceException || ex instanceof ClientApiResponseParseException);
    }
}
