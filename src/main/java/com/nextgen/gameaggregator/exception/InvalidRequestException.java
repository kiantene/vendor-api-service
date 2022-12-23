package com.nextgen.gameaggregator.exception;

public class InvalidRequestException extends Exception {
    public InvalidRequestException() {
        super();
    }

    public InvalidRequestException(String message) {
        super(message);
    }
}
