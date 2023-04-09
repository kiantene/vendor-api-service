package com.nextgen.gameaggregator.util;

import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidUrlException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
public class ValidationUtils {


    public static final String ALPHANUMERIC_REGEX = "^[a-zA-Z0-9]+$";
    public static final String ALPHANUMERIC_DASH_REGEX = "^[a-zA-Z0-9_-]+$";
    public static final String UUID_REGEX = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
    public static final String ALPHANUMERIC_DASH_COLON_REGEX = "^[a-zA-Z0-9:_-]+$";
    public static final String WEB_OR_H5 = "^web|WEB|h5|H5+$";

    public static <T> void validateRequest(T requestObject) throws InvalidRequestException {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            Set<ConstraintViolation<T>> violations = validator.validate(requestObject);
            Map<String, String> validation = new HashMap<>();

            violations.forEach(v -> {
                String fieldName = v.getPropertyPath().toString();
                if (!validation.containsKey(fieldName)) {
                    if (v.getMessage().equals(v.getMessageTemplate())) {
                        validation.put(fieldName, v.getMessage());
                    } else {
                        validation.put(fieldName, null);
                    }
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

    public static void isValidUrl(String url) throws InvalidUrlException {
        UrlValidator validator = new UrlValidator();
        if (!validator.isValid(url)) {
            throw new InvalidUrlException();
        }
    }






}
