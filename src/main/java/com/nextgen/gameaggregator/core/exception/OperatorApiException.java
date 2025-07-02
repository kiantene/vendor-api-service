package com.nextgen.gameaggregator.core.exception;

public class OperatorApiException extends RuntimeException {

    private final Integer statusCode;         // HTTP status code if applicable
    private final String responseBody;        // Raw response body from operator
    private final String url;                 // Endpoint called

    public OperatorApiException(String message) {
        super(message);
        this.statusCode = null;
        this.responseBody = null;
        this.url = null;
    }

    public OperatorApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.responseBody = null;
        this.url = null;
    }

    public OperatorApiException(String message, String url) {
        super(message);
        this.url = url;
        this.statusCode = null;
        this.responseBody = null;
    }

    public OperatorApiException(String message, String url, Integer statusCode, String responseBody) {
        super(message);
        this.url = url;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public OperatorApiException(String message, String url, Integer statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.url = url;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getUrl() {
        return url;
    }

    public Throwable getRootCause() {
        Throwable cause = this;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
