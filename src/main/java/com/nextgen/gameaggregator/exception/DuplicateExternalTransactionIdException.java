package com.nextgen.gameaggregator.exception;

public class DuplicateExternalTransactionIdException extends Exception {
    public DuplicateExternalTransactionIdException() {
        super();
    }

    public DuplicateExternalTransactionIdException(String message) {
        super(message);
    }
}
