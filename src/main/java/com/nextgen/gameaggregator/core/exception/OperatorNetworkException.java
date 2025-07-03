package com.nextgen.gameaggregator.core.exception;

public class OperatorNetworkException extends OperatorApiException {
    public OperatorNetworkException(String message, String url, Throwable cause) {
        super(message, url, cause);
    }
}
