package com.nextgen.gameaggregator.core.security.signature;

import java.util.Map;

public record ValidationResult(
        boolean valid,
        Map<String,String> additionalFields,
        boolean isSkipped
) {
    public static ValidationResult success(Map<String,String> fields) {
        return new ValidationResult(true, fields != null ? fields : Map.of(), false);
    }

    public static ValidationResult success() {
        return new ValidationResult(true, Map.of(), false);
    }

    public static ValidationResult failure() {
        return new ValidationResult(false, Map.of(), false);
    }

    public static ValidationResult skipped() {
        return new ValidationResult(true, Map.of(), true);
    }
}
