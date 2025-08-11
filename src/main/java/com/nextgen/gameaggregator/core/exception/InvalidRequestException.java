package com.nextgen.gameaggregator.core.exception;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashMap;
import java.util.Map;

public class InvalidRequestException extends RuntimeException {

    private final Map<String, String> fieldErrors;
    private boolean showFieldErrors = true;

    public InvalidRequestException(String message) {
        this(message, new HashMap<>());
    }

    public InvalidRequestException(MethodArgumentNotValidException cause) {
        this("Request validation failed", cause);
    }

    public InvalidRequestException(String message, MethodArgumentNotValidException cause) {
        super(message, cause);
        this.fieldErrors = extractFieldErrors(cause);
    }

    public InvalidRequestException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    public InvalidRequestException(String message, Map<String, String> fieldErrors, Throwable cause) {
        super(message, cause);
        this.fieldErrors = fieldErrors;
    }

    private static Map<String, String> extractFieldErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return errors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }

    public boolean isShowFieldErrors() {
        return showFieldErrors;
    }

    public void setShowFieldErrors(boolean showFieldErrors) {
        this.showFieldErrors = showFieldErrors;
    }
}
