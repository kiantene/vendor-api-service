package com.nextgen.gameaggregator.custodianseamless.exception;

public class WalletServiceTimeoutException extends Exception {

    private Integer WalletStatus = null;

    public WalletServiceTimeoutException() {
        super();
    }

    public WalletServiceTimeoutException(String message) {
        super(message);
    }

    public WalletServiceTimeoutException(Integer walletStatus) {
        WalletStatus = walletStatus;
    }

    public WalletServiceTimeoutException(String message, Integer walletStatus) {
        super(message);
        WalletStatus = walletStatus;
    }

    public WalletServiceTimeoutException(String message, Throwable cause, Integer walletStatus) {
        super(message, cause);
        WalletStatus = walletStatus;
    }

    public WalletServiceTimeoutException(Throwable cause, Integer walletStatus) {
        super(cause);
        WalletStatus = walletStatus;
    }

    public WalletServiceTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Integer walletStatus) {
        super(message, cause, enableSuppression, writableStackTrace);
        WalletStatus = walletStatus;
    }

    public Integer getWalletStatus() {
        return this.WalletStatus;
    }

}