package com.nextgen.gameaggregator.exception;

public class ExceedThresholdCounterException extends Exception {
    public ExceedThresholdCounterException() {
        super();
    }
    public ExceedThresholdCounterException(String message) {
        super(message);
    }
}