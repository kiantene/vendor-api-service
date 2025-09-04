package com.nextgen.gameaggregator.core.exception;

public class GameSessionExpiredException extends RuntimeException {
    public GameSessionExpiredException() { super(); }

    public GameSessionExpiredException(String message) {
        super(message);
    }
}
