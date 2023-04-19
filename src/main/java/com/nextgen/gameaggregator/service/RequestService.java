package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.exception.HttpResponseStatusCodeException;
import com.nextgen.gameaggregator.exception.InvalidResponseException;
import com.nextgen.gameaggregator.util.RequestLogVo;
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
public class RequestService {

    public void validateVendorHttpStatusResponse(ResponseEntity responseEntity) throws HttpResponseStatusCodeException {
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

    public static void failResponseLog(RequestLogVo requestLogVo, Exception exception) {
        Gson gson = new Gson();
        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put("ApiUrl: ", requestLogVo.getCallbackUrl() + requestLogVo.getEndpoint());
        logInfo.put("Header: ", requestLogVo.getResponseEntity().getHeaders());
        logInfo.put("RequestParam: ", requestLogVo.getRequestObject());
        logInfo.put("Response: ", requestLogVo.getResponseEntity().getBody());
        logInfo.put("HttpStatusCode: ", requestLogVo.getResponseEntity().getStatusCode());
        logInfo.put("ServicePackage: ", requestLogVo.getPackageName());
        logInfo.put("ExceptionName: ", exception.getClass().getName());
        logInfo.put("ExceptionMessage: ", exception.getMessage());
        log.error(gson.toJson(logInfo));
    }

    public static void successResponseLog(RequestLogVo requestLogVo) {
        Gson gson = new Gson();
        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put("ApiUrl: ", requestLogVo.getCallbackUrl() + requestLogVo.getEndpoint());
        logInfo.put("Header: ", requestLogVo.getResponseEntity().getHeaders());
        logInfo.put("RequestParam: ", requestLogVo.getRequestObject());
        logInfo.put("Response: ", requestLogVo.getResponseEntity().getBody());
        logInfo.put("HttpStatusCode: ", requestLogVo.getResponseEntity().getStatusCode());
        logInfo.put("ServicePackage: ", requestLogVo.getPackageName());
        if ((requestLogVo.getProfilesActive().equals("dev")) ||
                (requestLogVo.getProfilesActive().equals("qa")) ||
                (requestLogVo.getProfilesActive().equals("stg"))) {
            log.info(gson.toJson(logInfo));
        }
    }


    public RequestLogVo createRequestLogVo(String endpoint, String callbackUrl, Object requestObject,
                                           ResponseEntity responseEntity, String packageName, String profilesActive) {
        RequestLogVo requestLogVo = new RequestLogVo();
        requestLogVo.setEndpoint(endpoint);
        requestLogVo.setCallbackUrl(callbackUrl);
        requestLogVo.setRequestObject(requestObject);
        requestLogVo.setResponseEntity(responseEntity);
        requestLogVo.setPackageName(packageName);
        requestLogVo.setProfilesActive(profilesActive);
        return requestLogVo;
    }
}
