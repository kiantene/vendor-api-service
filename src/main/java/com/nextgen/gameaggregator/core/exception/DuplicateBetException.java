package com.nextgen.gameaggregator.core.exception;

public class DuplicateBetException extends RuntimeException {
    public DuplicateBetException() { super(); }

    public DuplicateBetException(String message) {
        super(message);
    }
}
