package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;

public class DuplicateRequestException extends RuntimeException {
    private GameTransaction transaction;

    public DuplicateRequestException() { super(); }

    public DuplicateRequestException(String message) {
        super(message);
    }

    public DuplicateRequestException(String message, Throwable ex) {
        super(message, ex);
    }

    public DuplicateRequestException(String message, GameTransaction txn) {
        super(message);
        this.transaction = txn;
    }

    public GameTransaction getTransaction() {
        return this.transaction;
    }
}
