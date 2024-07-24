package com.nextgen.gameaggregator.exception;

import java.util.Map;

public class InvalidFormatException extends Exception {
    private Map<String, String> validation;

    public InvalidFormatException() {
        super();
    }

    public InvalidFormatException(String message) {
        super(message);
    }

    public InvalidFormatException(Map<String, String> validation) {
        super();
        this.validation = validation;
    }

    public String getAllValidationErrorMessages() {
        return String.join(", ", this.getValidation().values());
    }

    public Map<String, String> getValidation() {
        return this.validation;
    }
}
