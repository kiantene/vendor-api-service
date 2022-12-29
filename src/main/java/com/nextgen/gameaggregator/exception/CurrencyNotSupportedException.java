package com.nextgen.gameaggregator.exception;

public class CurrencyNotSupportedException extends Exception {
    public CurrencyNotSupportedException() {
        super();
    }

    public CurrencyNotSupportedException(String message) {
        super(message);
    }
}
