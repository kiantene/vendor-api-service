package com.nextgen.gameaggregator.util;

import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class ValidationUtils {

    public static final String ALPHANUMERIC_REGEX = "^[a-zA-Z0-9]+$";
    public static final String ALPHANUMERIC_DASH_REGEX = "^[a-zA-Z0-9_-]+$";

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

    public static <X extends Throwable> void validateLength(String value, final int min, final int max, Supplier<? extends X> exceptionSupplier) throws X {
        if (value.length() < min || value.length() > max) {
            throw exceptionSupplier.get();
        }
    }

    public static void isEquals(String expected, String actual) throws InvalidRequestException {
        isEquals(expected, actual, InvalidRequestException::new);
    }

    public static <X extends Throwable> void isEquals(String expected, String actual, Supplier<? extends X> exceptionSupplier) throws X {
        if (!expected.equalsIgnoreCase(actual)) {
            throw exceptionSupplier.get();
        }
    }
}
