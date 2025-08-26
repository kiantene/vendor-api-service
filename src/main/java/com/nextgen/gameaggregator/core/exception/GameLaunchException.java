package com.nextgen.gameaggregator.core.exception;

public class GameLaunchException extends RuntimeException {
    public GameLaunchException() {
        super();
    }

    public GameLaunchException(String message) {
        super(message);
    }

    public GameLaunchException(String message, Throwable cause) {
        super(message, cause);
    }
}
