package com.nextgen.gameaggregator.exception;

import java.util.Map;

public class InvalidRequestException extends Exception {
    private Map<String, String> validation;
    public InvalidRequestException() {
        super();
    }

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(Map<String, String> validation) {
        super();
        this.validation = validation;
    }

    public Map<String, String> getValidation() {
        return this.validation;
    }
}
