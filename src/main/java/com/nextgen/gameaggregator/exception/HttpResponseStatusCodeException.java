package com.nextgen.gameaggregator.exception;

public class HttpResponseStatusCodeException extends Exception {
    public HttpResponseStatusCodeException() {
        super();
    }

    public HttpResponseStatusCodeException(String message) {
        super(message);
    }
}
