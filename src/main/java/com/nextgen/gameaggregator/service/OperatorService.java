package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorLogVo;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class OperatorService {

    public WalletBalanceVo responseOperatorSub() {
        WalletBalanceVo.ResponseData responseData = new WalletBalanceVo.ResponseData();
        responseData.setBalance(BigDecimal.ONE);
        WalletBalanceVo balanceVo = new WalletBalanceVo();
        balanceVo.setData(responseData);
        return balanceVo;
    }

    public void validateOperatorHttpStatusResponse(ResponseEntity responseEntity) throws HttpResponseStatusCodeException {
        if (responseEntity.getStatusCodeValue() != 200) {
            throw new HttpResponseStatusCodeException();
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
                throw new InvalidOperatorResponseException(validation.toString());
            }
        }

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

    public void validateResponseUserNameAndCurrency(WalletBalanceVo walletBalanceVo, String userName, String currency) throws InvalidOperatorResponseException {
        Map<String, String> validation = new HashMap<>();

        if (walletBalanceVo.getStatus().equals(ResponseCodes.Status.SC_OK)) {
            if (!walletBalanceVo.getData().getUsername().equals(userName)) {
                validation.put("username", "username not match");
            }

            if (!walletBalanceVo.getData().getCurrency().equals(currency)) {
                validation.put("currency", "currency not match");
            }
        }

        if (!validation.isEmpty()) { // Missing/Invalid request parameters
            throw new InvalidOperatorResponseException(validation.toString());
        }
    }

    public void operatorStatusException(ResponseCodes.Status operatorStatus) throws
            InvalidSignatureException, InvalidRequestException, DuplicateRequestException, InvalidPlayerException,
            InvalidCurrencyException, InvalidTokenException, DisabledAgentPlayerException, SystemMaintenanceException,
            InvalidOperatorResponseException, InsufficientBalanceException, GameNotSupportedException,
            DuplicateTransactionException, BetNotFoundException {
        switch (operatorStatus) {
            case SC_INVALID_SIGNATURE -> {
                throw new InvalidSignatureException();
            }
            case SC_INVALID_REQUEST -> {
                throw new InvalidRequestException();
            }
            case SC_DUPLICATE_REQUEST -> {
                throw new DuplicateRequestException();
            }
            case SC_USER_NOT_EXISTS -> {
                throw new InvalidPlayerException();
            }
            case SC_WRONG_CURRENCY -> {
                throw new InvalidCurrencyException();
            }
            case SC_INVALID_TOKEN -> {
                throw new InvalidTokenException();
            }
            case SC_USER_DISABLED -> {
                throw new DisabledAgentPlayerException();
            }
            case SC_UNDER_MAINTENANCE -> {
                throw new SystemMaintenanceException();
            }
            case SC_UNKNOWN_ERROR -> {
                throw new InvalidOperatorResponseException();
            }
            case SC_INSUFFICIENT_FUNDS -> {
                throw new InsufficientBalanceException();
            }
            case SC_INVALID_GAME -> {
                throw new GameNotSupportedException();
            }
            case SC_TRANSACTION_DUPLICATED -> {
                throw new DuplicateTransactionException();
            }
            case SC_TRANSACTION_NOT_EXISTS -> {
                throw new BetNotFoundException();
            }
        }
    }


    //region logging
    public static void failResponseLog(OperatorLogVo operatorLogVo, String exceptionName) {
        Gson gson = new Gson();
        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put("CallbackUrl: ", operatorLogVo.getCallbackUrl());
        logInfo.put("RequestParam: ", operatorLogVo.getRequestObject());
        logInfo.put("Response: ", operatorLogVo.getResponseEntity().getBody());
        logInfo.put("HttpStatusCode: ", operatorLogVo.getResponseEntity().getStatusCodeValue());
        logInfo.put("OperatorService: ", operatorLogVo.getEndpoint());
        logInfo.put("RequestSignature: ", operatorLogVo.getSignature());
        logInfo.put("ExceptionName: ", exceptionName);
        log.error(gson.toJson(logInfo));
    }

    public static void successResponseLog(OperatorLogVo operatorLogVo) {
        Gson gson = new Gson();
        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put("CallbackUrl: ", operatorLogVo.getCallbackUrl());
        logInfo.put("RequestParam: ", operatorLogVo.getRequestObject());
        logInfo.put("Response: ", operatorLogVo.getResponseEntity().getBody());
        logInfo.put("HttpStatusCode: ", operatorLogVo.getResponseEntity().getStatusCodeValue());
        logInfo.put("OperatorService: ", operatorLogVo.getEndpoint());
        logInfo.put("RequestSignature: ", operatorLogVo.getSignature());
        if ((operatorLogVo.getProfilesActive().equals("dev")) ||
                (operatorLogVo.getProfilesActive().equals("qa")) ||
                (operatorLogVo.getProfilesActive().equals("stg"))) {
            log.info(gson.toJson(logInfo));
        }
    }


    public static void operatorResponseLogging(
            Boolean isSuccess, String endpoint, String callbackUrl, Object dto, String responseString, String profilesActive) {
        Gson gson = new Gson();

        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put("callbackUrl ", callbackUrl);
        logInfo.put("ApiParam ", dto);
        logInfo.put("ApiResponse ", responseString);
        if (!isSuccess) {
            logInfo.put("Operator Service Error ", endpoint);
            log.error(gson.toJson(logInfo));
        } else {
            if ((profilesActive.equals("dev")) ||
                    (profilesActive.equals("qa")) ||
                    (profilesActive.equals("stg"))) {
                logInfo.put("Operator Service Success ", endpoint);
                log.info(gson.toJson(logInfo));
            }
        }
    }

    public OperatorLogVo createOperatorLogVo(String endpoint, String callbackUrl, Object requestObject, ResponseEntity responseEntity, String signature, String profilesActive) {
        OperatorLogVo operatorLogVo = new OperatorLogVo();
        operatorLogVo.setEndpoint(endpoint);
        operatorLogVo.setCallbackUrl(callbackUrl);
        operatorLogVo.setRequestObject(requestObject);
        operatorLogVo.setResponseEntity(responseEntity);
        operatorLogVo.setSignature(signature);
        operatorLogVo.setProfilesActive(profilesActive);
        return operatorLogVo;
    }
    //endregion
}
