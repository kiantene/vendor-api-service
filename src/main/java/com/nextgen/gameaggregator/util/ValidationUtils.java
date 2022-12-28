package com.nextgen.gameaggregator.util;

import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ValidationUtils {
    public static <T> void validateRequest(T requestObject) throws InvalidRequestException {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            Set<ConstraintViolation<T>> violations = validator.validate(requestObject);
            Map<String, String> validation = new HashMap<>();

            violations.forEach(v -> {
                String fieldName = v.getPropertyPath().toString();
                if (!validation.containsKey(fieldName)) {
                    validation.put(fieldName, "Invalid value.");
                }
            });

            if (!validation.isEmpty()) { // Missing/Invalid request parameters
                throw new InvalidRequestException(validation);
            }
        }
    }

    public static void validateVendorUsername(String username) throws InvalidPlayerException {
        // Max length is based on database type length
        final int min = 3;
        final int max = 100;

        if (username.length() < min || username.length() > max) {
            throw new InvalidPlayerException();
        }
    }

    public static void validateEquals(String expected, String actual) throws InvalidRequestException {
        if (!expected.equals(actual)) {
            throw new InvalidRequestException();
        }
    }

    public static void validateDecimalLength(BigDecimal decimal, int maxDigits, int maxScale) throws InvalidRequestException {
        BigDecimal decimalNoZeros = decimal.stripTrailingZeros();
        int scale = decimalNoZeros.scale();
        int digits = decimalNoZeros.precision() - scale;
        if (digits > maxDigits || scale > maxScale) {
            throw new InvalidRequestException();
        }
    }
}
