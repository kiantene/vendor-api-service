package com.nextgen.gameaggregator.core.exception;

public class Http4xxException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public Http4xxException(int statusCode, String responseBody) {
        super("4xx Error: " + statusCode + " - " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
