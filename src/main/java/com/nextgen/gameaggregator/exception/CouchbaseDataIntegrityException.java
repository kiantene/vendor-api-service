package com.nextgen.gameaggregator.exception;

public class CouchbaseDataIntegrityException extends Exception {
    public CouchbaseDataIntegrityException() {
        super();
    }

    public CouchbaseDataIntegrityException(String message) {
        super(message);
    }
}
