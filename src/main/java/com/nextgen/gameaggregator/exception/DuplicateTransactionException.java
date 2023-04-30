package com.nextgen.gameaggregator.exception;

public class DuplicateTransactionException extends Exception {
    public DuplicateTransactionException() {
        super();
    }

    public DuplicateTransactionException(String message) {
        super(message);
    }
}
