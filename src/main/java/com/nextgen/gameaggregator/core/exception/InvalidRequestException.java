package com.nextgen.gameaggregator.core.exception;

import jakarta.validation.ConstraintViolation;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class InvalidRequestException extends RuntimeException {
    // Store the set of constraint violations
    private final Set<ConstraintViolation<?>> violations;

    public InvalidRequestException(String message) {
        // Create a user-friendly message from violations
        super(message);
        this.violations = new HashSet<>();
    }

    public InvalidRequestException(Set<ConstraintViolation<?>> violations) {
        // Create a user-friendly message from violations
        super("Validation failed for object: " +
                violations.stream()
                        .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                        .collect(Collectors.joining(", ")));
        this.violations = violations;
    }

    /**
     * Provides a map of field names to error messages for easier processing in error responses.
     * If a violation is not field-specific (e.g., class-level constraint), its property path
     * might be empty or represent the class name.
     *
     * @return A map where keys are field names (or object name) and values are error messages.
     */
    public java.util.Map<String, String> getFieldErrors() {
        return violations.stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(), // Key: field name
                        ConstraintViolation::getMessage, // Value: error message
                        (existing, replacement) -> existing // In case of duplicate keys, keep the first message
                ));
    }
}
