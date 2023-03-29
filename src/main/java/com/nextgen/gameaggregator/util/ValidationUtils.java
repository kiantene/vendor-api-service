package com.nextgen.gameaggregator.util;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;

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

    public static <T> void validateResponse(T requestObject) throws InvalidOperatorResponseException {
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
                System.err.println("CCCCCC");
                throw new InvalidOperatorResponseException(validation.toString());
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

    public static void operatorResponseLogging(Boolean isSuccess, String endpoint, String callbackUrl, Object dto, String responseString) {
        Gson gson = new Gson();

        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put("callbackUrl ", callbackUrl);
        logInfo.put("ApiParam ", dto);
        logInfo.put("ApiResponse ", responseString);
        if (!isSuccess) {
            logInfo.put("Operator Service Error ", endpoint);
            log.error(gson.toJson(logInfo));
        } else {
            if ((System.getProperty("spring.profiles.active") == "dev") ||
                    (System.getProperty("spring.profiles.active") == "qa") ||
                    (System.getProperty("spring.profiles.active") == "stg")) {
                logInfo.put("Operator Service Success ", endpoint);
                log.info(gson.toJson(logInfo));
            }
        }

    }
}
