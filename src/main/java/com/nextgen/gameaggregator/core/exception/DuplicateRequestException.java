package com.nextgen.gameaggregator.core.exception;

import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;

import java.math.BigDecimal;

public class DuplicateRequestException extends RuntimeException {
    private GameTransaction transaction;
    private RequestIdempotentLog requestLog;


    public DuplicateRequestException() {
        super();
    }

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

    public DuplicateRequestException(String message, RequestIdempotentLog requestLog) {
        super(message);
        this.requestLog = requestLog;
    }

    public GameTransaction getTransaction() {
        return this.transaction;
    }

    public String getTransactionId() {
        if (transaction != null) {
            return transaction.getTransactionId();
        }
        if (requestLog != null) {
            return requestLog.getTransactionId();
        }
        return null;
    }

    public String getCurrency() {
        if (transaction != null) {
            return transaction.getCurrency();
        }
        if (requestLog != null) {
            return requestLog.getCurrency();
        }
        return null;
    }

    public BigDecimal getBalance() {
        if (this.transaction != null && this.transaction.getBalance() != null) {
            return this.transaction.getBalance();
        }
        if (requestLog != null && requestLog.getBalance() != null) {
            return requestLog.getBalance();
        }
        return BigDecimal.ZERO;   // matches pre-MR behaviour in PGSoftExceptionMapper
    }

}
