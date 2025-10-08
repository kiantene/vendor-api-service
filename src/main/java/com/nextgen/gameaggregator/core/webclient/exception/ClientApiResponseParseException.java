package com.nextgen.gameaggregator.core.webclient.exception;

public class ClientApiResponseParseException extends RuntimeException {
    private final String rawResponse;

    public ClientApiResponseParseException(String message, String rawResponse, Throwable cause) {
        super(message, cause);
        this.rawResponse = rawResponse;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
