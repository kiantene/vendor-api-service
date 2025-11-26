package com.nextgen.gameaggregator.core.exception;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException() {
        super();
    }

    public PlayerNotFoundException(String message) {
        super(message);
    }
}
