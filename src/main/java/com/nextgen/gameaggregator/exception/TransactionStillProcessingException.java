package com.nextgen.gameaggregator.exception;

@Deprecated
public class TransactionStillProcessingException extends Exception {
    public TransactionStillProcessingException() {
        super();
    }

    public TransactionStillProcessingException(String message) {
        super(message);
    }
}
