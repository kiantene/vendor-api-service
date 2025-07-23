package com.nextgen.gameaggregator.core.exception;

public class CurrencyNotFoundException extends RuntimeException {
    public CurrencyNotFoundException(String message) {
        super(message);
    }

    public CurrencyNotFoundException() {
    }
}
