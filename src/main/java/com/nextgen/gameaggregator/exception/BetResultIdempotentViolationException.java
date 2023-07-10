package com.nextgen.gameaggregator.exception;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.entity.RawBetResultLog;

import java.math.BigDecimal;

public class BetResultIdempotentViolationException extends Exception {
    private Long vendorSettleTime;
    private BigDecimal balance;
    private String transactionId;
    private String betId;

    public BetResultIdempotentViolationException() {
        super();
    }

    public BetResultIdempotentViolationException(String message) {
        super(message);
    }

    public BetResultIdempotentViolationException(BetInformation betInformation) {
        super();

        this.vendorSettleTime = betInformation.getVendorSettleTime();
        this.balance = betInformation.getBalance();
        this.betId = betInformation.getBetId();
        this.transactionId = betInformation.getInternalTransactionId();
    }

    public BetResultIdempotentViolationException(RawBetResultLog rawBetResultLog) {
        super();

        this.vendorSettleTime = System.currentTimeMillis();
        this.balance = rawBetResultLog.getBalance();
        this.transactionId = rawBetResultLog.getResultLogId();
    }

    public Long getVendorSettleTime() { return this.vendorSettleTime; }
    public BigDecimal getBalance() { return this.balance; }

    public String getBetId() { return this.betId; }
    public String getTransactionId() { return this.transactionId; }
}
