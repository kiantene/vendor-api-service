package com.nextgen.gameaggregator.exception;

public class DatabaseMoreThanOneRecordException extends Exception {
    public DatabaseMoreThanOneRecordException() {
        super();
    }

    public DatabaseMoreThanOneRecordException(String message) {
        super(message);
    }
}
