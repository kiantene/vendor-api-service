package com.nextgen.gameaggregator.util;

import com.nextgen.gameaggregator.exception.InvalidRequestException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidationUtils {
    public static <T> void validateRequest(T requestObject) throws InvalidRequestException {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            Set<ConstraintViolation<T>> violations = validator.validate(requestObject);

            Map<String, String> validation = violations.stream()
                    .collect(Collectors.toMap(k -> k.getPropertyPath().toString(), v -> "Invalid value."));

            if (!validator.validate(requestObject).isEmpty()) { // Missing/Invalid request parameters
                throw new InvalidRequestException(validation);
            }
        }
    }
}
