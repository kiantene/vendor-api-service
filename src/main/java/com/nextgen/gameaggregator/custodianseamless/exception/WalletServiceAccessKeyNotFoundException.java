package com.nextgen.gameaggregator.custodianseamless.exception;


public class WalletServiceAccessKeyNotFoundException extends Exception {

    private Integer WalletStatus = null;

    public WalletServiceAccessKeyNotFoundException() {
        super();
    }

    public WalletServiceAccessKeyNotFoundException(String message) {
        super(message);
    }

    public WalletServiceAccessKeyNotFoundException(Integer walletStatus) {
        WalletStatus = walletStatus;
    }

    public WalletServiceAccessKeyNotFoundException(String message, Integer walletStatus) {
        super(message);
        WalletStatus = walletStatus;
    }

    public WalletServiceAccessKeyNotFoundException(String message, Throwable cause, Integer walletStatus) {
        super(message, cause);
        WalletStatus = walletStatus;
    }

    public WalletServiceAccessKeyNotFoundException(Throwable cause, Integer walletStatus) {
        super(cause);
        WalletStatus = walletStatus;
    }

    public WalletServiceAccessKeyNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Integer walletStatus) {
        super(message, cause, enableSuppression, writableStackTrace);
        WalletStatus = walletStatus;
    }

    public Integer getWalletStatus() {
        return this.WalletStatus;
    }

}