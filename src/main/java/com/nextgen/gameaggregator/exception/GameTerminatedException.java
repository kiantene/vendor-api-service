package com.nextgen.gameaggregator.exception;

@Deprecated
public class GameTerminatedException extends Exception {

    public GameTerminatedException() {
        super();
    }

    public GameTerminatedException(String message) {
        super(message);
    }
}
