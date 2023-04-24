package com.nextgen.gameaggregator.exception;

public class ResponseNotMatchRequestException extends Exception {
    public ResponseNotMatchRequestException() {
        super();
    }

    public ResponseNotMatchRequestException(String message) {
        super(message);
    }
}

