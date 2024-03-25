package com.nextgen.gameaggregator.custodianseamless.exception;

public class InvalidWalletServiceResponseException extends Exception {

    private Integer WalletStatus = null;

    public InvalidWalletServiceResponseException() {
        super();
    }

    public InvalidWalletServiceResponseException(String message) {
        super(message);
    }

    public InvalidWalletServiceResponseException(Integer walletStatus) {
        WalletStatus = walletStatus;
    }

    public InvalidWalletServiceResponseException(String message, Integer walletStatus) {
        super(message);
        WalletStatus = walletStatus;
    }

    public InvalidWalletServiceResponseException(String message, Throwable cause, Integer walletStatus) {
        super(message, cause);
        WalletStatus = walletStatus;
    }

    public InvalidWalletServiceResponseException(Throwable cause, Integer walletStatus) {
        super(cause);
        WalletStatus = walletStatus;
    }

    public InvalidWalletServiceResponseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Integer walletStatus) {
        super(message, cause, enableSuppression, writableStackTrace);
        WalletStatus = walletStatus;
    }

    public Integer getWalletStatus() {
        return this.WalletStatus;
    }

}