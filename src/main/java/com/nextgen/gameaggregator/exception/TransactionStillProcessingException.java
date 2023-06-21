package com.nextgen.gameaggregator.exception;

public class TransactionStillProcessingException extends Exception {
    public TransactionStillProcessingException() {
        super();
    }

    public TransactionStillProcessingException(String message) {
        super(message);
    }
}
