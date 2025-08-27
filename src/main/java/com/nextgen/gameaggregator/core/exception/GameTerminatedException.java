package com.nextgen.gameaggregator.core.exception;

public class GameTerminatedException extends RuntimeException {
    public GameTerminatedException() { super(); }

    public GameTerminatedException(String message) {
        super(message);
    }
}
