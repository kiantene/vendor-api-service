package com.nextgen.gameaggregator.custodianseamless.service;

import com.nextgen.gameaggregator.exception.HttpResponseStatusCodeException;
import com.nextgen.gameaggregator.exception.InvalidResponseException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Service
@Slf4j
public class WalletRequestValidationService {

//    public static void walletStatusException(ResponseCodes.Status walletStatus) throws InvalidWalletServiceResponseException {
//
//        if ((!walletStatus.equals(ResponseCodes.Status.SC_OK)) && (!walletStatus.equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS))) {
//            throw new InvalidWalletServiceResponseException(walletStatus.code);
//        }
//    }


    public static void validateVendorHttpStatusResponse(ResponseEntity responseEntity) throws HttpResponseStatusCodeException {
        if (responseEntity.getStatusCode().isError()) {
            throw new HttpResponseStatusCodeException("HTTP status Code Error");
        }
    }

    public static <T> void validateResponse(T requestObject) throws InvalidResponseException {
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
                throw new InvalidResponseException(validation.toString());
            }
        }
    }
}
