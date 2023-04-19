package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.exception.HttpResponseStatusCodeException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.vendor.VendorLogVo;
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
public class VendorRequestService {

    public void validateVendorHttpStatusResponse(ResponseEntity responseEntity) throws HttpResponseStatusCodeException {
        if (responseEntity.getStatusCode().isError()) {
            throw new HttpResponseStatusCodeException("HTTP status Code Error");
        }
    }

    public static <T> void validateResponse(T requestObject) throws InvalidVendorResponseException {
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
                throw new InvalidVendorResponseException(validation.toString());
            }
        }

    }

    public static void failResponseLog(VendorLogVo vendorLogVo, Exception exception) {
        Gson gson = new Gson();
        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put("CallbackUrl: ", vendorLogVo.getCallbackUrl());
        logInfo.put("Header: ", vendorLogVo.getResponseEntity().getHeaders());
        logInfo.put("RequestParam: ", vendorLogVo.getRequestObject());
        logInfo.put("Response: ", vendorLogVo.getResponseEntity().getBody());
        logInfo.put("HttpStatusCode: ", vendorLogVo.getResponseEntity().getStatusCode());
        logInfo.put("VendorService: ", vendorLogVo.getEndpoint());
        logInfo.put("ExceptionName: ", exception.getClass().getName());
        logInfo.put("ExceptionMessage: ", exception.getMessage());
        log.error(gson.toJson(logInfo));
    }

    public static void successResponseLog(VendorLogVo vendorLogVo) {
        Gson gson = new Gson();
        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put("CallbackUrl: ", vendorLogVo.getCallbackUrl());
        logInfo.put("Header: ", vendorLogVo.getResponseEntity().getHeaders());
        logInfo.put("RequestParam: ", vendorLogVo.getRequestObject());
        logInfo.put("Response: ", vendorLogVo.getResponseEntity().getBody());
        logInfo.put("HttpStatusCode: ", vendorLogVo.getResponseEntity().getStatusCode());
        logInfo.put("VendorService: ", vendorLogVo.getEndpoint());
        if ((vendorLogVo.getProfilesActive().equals("dev")) ||
                (vendorLogVo.getProfilesActive().equals("qa")) ||
                (vendorLogVo.getProfilesActive().equals("stg"))) {
            log.info(gson.toJson(logInfo));
        }
    }

    public VendorLogVo createVendorLogVo(String endpoint, String callbackUrl, Object requestObject, ResponseEntity responseEntity, String profilesActive) {
        VendorLogVo vendorLogVo = new VendorLogVo();
        vendorLogVo.setEndpoint(endpoint);
        vendorLogVo.setCallbackUrl(callbackUrl);
        vendorLogVo.setRequestObject(requestObject);
        vendorLogVo.setResponseEntity(responseEntity);
        vendorLogVo.setProfilesActive(profilesActive);
        return vendorLogVo;
    }
}
