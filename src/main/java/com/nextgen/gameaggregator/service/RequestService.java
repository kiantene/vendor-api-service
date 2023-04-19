package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.exception.HttpResponseStatusCodeException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidResponseException;
import com.nextgen.gameaggregator.exception.ResponseNotMatchRequestException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.util.RequestLogVo;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Service
@Slf4j
public class RequestService {

    //region operator use only
    public WalletBalanceVo responseOperatorSub() {
        WalletBalanceVo.ResponseData responseData = new WalletBalanceVo.ResponseData();
        responseData.setBalance(BigDecimal.ONE);
        WalletBalanceVo balanceVo = new WalletBalanceVo();
        balanceVo.setData(responseData);
        return balanceVo;
    }

    public void validateResponseMatchRequest(WalletBalanceVo walletBalanceVo, String userName, String currency, String traceId) throws ResponseNotMatchRequestException {
        Map<String, String> validation = new HashMap<>();

        if (walletBalanceVo.getStatus().equals(ResponseCodes.Status.SC_OK)) {
            if (!walletBalanceVo.getData().getUsername().equals(userName)) {
                validation.put("username", "username not match");
            }

            if (!walletBalanceVo.getData().getCurrency().equals(currency)) {
                validation.put("currency", "currency not match");
            }

            if (!walletBalanceVo.getTraceId().equals(traceId)) {
                validation.put("currency", "trace Id not match");
            }
        }

        if (!validation.isEmpty()) { // Missing/Invalid request parameters
            throw new ResponseNotMatchRequestException(validation.toString());
        }
    }

    public void operatorStatusException(ResponseCodes.Status operatorStatus) throws InvalidOperatorResponseException {

        if(!operatorStatus.equals(ResponseCodes.Status.SC_OK)){
            throw new InvalidOperatorResponseException(operatorStatus.code);
        }
//        switch (operatorStatus) {
//            case SC_INVALID_SIGNATURE -> {
//                throw new InvalidSignatureException();
//            }
//            case SC_INVALID_REQUEST -> {
//                throw new InvalidRequestException();
//            }
//            case SC_DUPLICATE_REQUEST -> {
//                throw new DuplicateRequestException();
//            }
//            case SC_USER_NOT_EXISTS -> {
//                throw new InvalidPlayerException();
//            }
//            case SC_WRONG_CURRENCY -> {
//                throw new InvalidCurrencyException();
//            }
//            case SC_INVALID_TOKEN -> {
//                throw new InvalidTokenException();
//            }
//            case SC_USER_DISABLED -> {
//                throw new DisabledAgentPlayerException();
//            }
//            case SC_UNDER_MAINTENANCE -> {
//                throw new SystemMaintenanceException();
//            }
//            case SC_UNKNOWN_ERROR -> {
//                throw new InvalidOperatorResponseException();
//            }
//            case SC_INSUFFICIENT_FUNDS -> {
//                throw new InsufficientBalanceException();
//            }
//            case SC_INVALID_GAME -> {
//                throw new GameNotSupportedException();
//            }
//            case SC_TRANSACTION_DUPLICATED -> {
//                throw new DuplicateTransactionException();
//            }
//            case SC_TRANSACTION_NOT_EXISTS -> {
//                throw new BetNotFoundException();
//            }
//        }
    }
    //endregion

    public Consumer<HttpHeaders> setHeaders(MultiValueMap<String, String> headersAsMap){
        LinkedMultiValueMap mvmap = new LinkedMultiValueMap<>(headersAsMap);
        Consumer<HttpHeaders> consumer = it -> it.addAll(mvmap);
        return consumer;
    }

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

            System.err.println(validation.toString());
            if (!validation.isEmpty()) { // Missing/Invalid request parameters
                throw new InvalidResponseException(validation.toString());
            }
        }

    }

    public static void failResponseLog(RequestLogVo requestLogVo, Exception exception) {
        Gson gson = new Gson();
        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put("ApiUrl: ", requestLogVo.getCallbackUrl() + requestLogVo.getEndpoint());
        logInfo.put("RequestHeaders: ", requestLogVo.getRequestHeaders());
        logInfo.put("RequestParam: ", requestLogVo.getRequestObject());
        logInfo.put("ResponseBody: ", requestLogVo.getResponseEntity().getBody());
        logInfo.put("ResponseHeaders: ", requestLogVo.getResponseEntity().getHeaders());
        logInfo.put("HttpStatusCode: ", requestLogVo.getResponseEntity().getStatusCode());
        logInfo.put("RequestStartTime: ", requestLogVo.getStartTime());
        logInfo.put("RequestEndTime: ", requestLogVo.getEndTime());
        logInfo.put("ServicePackage: ", requestLogVo.getPackageName());
        logInfo.put("ExceptionName: ", exception.getClass().getName());
        logInfo.put("ExecuteTimeMillis: ", requestLogVo.getEndTime() - requestLogVo.getStartTime());
        logInfo.put("ExceptionMessage: ", exception.getMessage());
        log.error(gson.toJson(logInfo));
    }

    public static void successResponseLog(RequestLogVo requestLogVo) {
        Gson gson = new Gson();
        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put("ApiUrl: ", requestLogVo.getCallbackUrl() + requestLogVo.getEndpoint());
        logInfo.put("RequestHeaders: ", requestLogVo.getRequestHeaders());
        logInfo.put("RequestParam: ", requestLogVo.getRequestObject());
        logInfo.put("ResponseBody: ", requestLogVo.getResponseEntity().getBody());
        logInfo.put("ResponseHeaders: ", requestLogVo.getResponseEntity().getHeaders());
        logInfo.put("HttpStatusCode: ", requestLogVo.getResponseEntity().getStatusCode());
        logInfo.put("RequestStartTime: ", requestLogVo.getStartTime());
        logInfo.put("RequestEndTime: ", requestLogVo.getEndTime());
        logInfo.put("ExecuteTimeMillis: ", requestLogVo.getEndTime() - requestLogVo.getStartTime());
        logInfo.put("ServicePackage: ", requestLogVo.getPackageName());
        if ((requestLogVo.getProfilesActive().equals("dev")) ||
                (requestLogVo.getProfilesActive().equals("qa")) ||
                (requestLogVo.getProfilesActive().equals("stg"))) {
            log.info(gson.toJson(logInfo));
        }
    }


    public RequestLogVo createRequestLogVo(String endpoint, String callbackUrl, Object requestObject,
                                           ResponseEntity responseEntity, MultiValueMap<String, String>  requestHeaders,
                                           Long startTime, Long endTime,
                                           String packageName, String profilesActive) {
        RequestLogVo requestLogVo = new RequestLogVo();
        requestLogVo.setEndpoint(endpoint);
        requestLogVo.setCallbackUrl(callbackUrl);
        requestLogVo.setRequestHeaders(requestHeaders);
        requestLogVo.setRequestObject(requestObject);
        requestLogVo.setResponseEntity(responseEntity);
        requestLogVo.setStartTime(startTime);
        requestLogVo.setEndTime(endTime);
        requestLogVo.setPackageName(packageName);
        requestLogVo.setProfilesActive(profilesActive);
        return requestLogVo;
    }


}
