package com.nextgen.gameaggregator.exception;

public class CouchbaseMoreThanOneRecordException extends Exception {
    public CouchbaseMoreThanOneRecordException() {
        super();
    }

    public CouchbaseMoreThanOneRecordException(String message) {
        super(message);
    }
}
