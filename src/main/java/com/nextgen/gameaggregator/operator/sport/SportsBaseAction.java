package com.nextgen.gameaggregator.operator.sport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.util.ApiSecurityUtils;
import com.nextgen.gameaggregator.util.ValidationUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SportsBaseAction {

    protected String endpoint;
    protected String requestType;

    public WalletBalanceVo callToOperator(WalletRequest walletRequest, Object dto)
            throws InvalidOperatorResponseException {

        walletRequest.setRequestType(this.requestType);
        // Validate Operator Dto
        ValidationUtils.doOperatorDtoValidation(dto);
        final String apiUrl = walletRequest.getOperatorEndpoint();
        final String signature = this.generateSignature(dto, walletRequest.getApiSecret());
        AtomicBoolean isTimeout = new AtomicBoolean(false);
        Long operatorEndTime = null;

        try {
            walletRequest.setOperatorEndpoint(apiUrl + this.endpoint);
            walletRequest.setOperatorData(new ObjectMapper().writeValueAsString(dto));
            walletRequest.setOperatorStart(System.currentTimeMillis());

            ResponseEntity<String> response = WebClient.create(apiUrl).post()
                    .uri(this.endpoint)
                    .header(EndPoints.HEADER_API_KEY, walletRequest.getApiKey())
                    .header(EndPoints.HEADER_SIGNATURE, signature)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .toEntity(String.class)
                    .retry(EndPoints.RETRY_COUNT)
                    .timeout(Duration.ofMillis(EndPoints.SPORTBOOK_TIMEOUT))
                    .onErrorResume(TimeoutException.class, e -> {
                        isTimeout.set(true);
                        return Mono.error(e);
                    })
                    .block();

            operatorEndTime = System.currentTimeMillis();

            WalletBalanceVo walletBalanceVo = this.validateResponse(response, isTimeout, walletRequest);

            walletRequest.setOperatorResponseStatus(walletBalanceVo.getStatus());

            return walletBalanceVo;

        } catch (JsonSyntaxException jsonSyntaxException) { // map to InvalidOperatorResponseException
            walletRequest.setErrorMessage(jsonSyntaxException.getMessage());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidOperatorResponseException exception) {
            walletRequest.setErrorMessage(exception.getMessage());
            throw exception; // re-throw to caller

        } catch (Exception exception) {
            walletRequest.setErrorMessage(exception.getMessage());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);

        } finally {
            walletRequest.setOperatorEnd(operatorEndTime);
        }
    }

    private WalletBalanceVo validateResponse(ResponseEntity<String> response, AtomicBoolean isTimeout, WalletRequest walletRequest)
            throws InvalidOperatorResponseException, InsufficientBalanceException {

        final Integer INVALID_RESPONSE = ResponseCodes.Status.SC_INVALID_RESPONSE.code;

        if (isTimeout.get()) {
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code);
        }

        if (response == null) throw new InvalidOperatorResponseException(INVALID_RESPONSE);

        HttpStatusCode httpStatusCode = response.getStatusCode();
        walletRequest.setOperatorHttpStatusCode(httpStatusCode.value());
        walletRequest.setOperatorResponse(response.getBody());

        // check is HTTP status code of the request is not 200
        if (httpStatusCode.isError()) {
            throw new InvalidOperatorResponseException(httpStatusCode.value());
        }

        WalletBalanceVo walletBalanceVo = new Gson().fromJson(response.getBody(), WalletBalanceVo.class);

        // check is operator responses success
        WalletBalanceVo.ResponseData responseData = walletBalanceVo.getData();

        if (!walletBalanceVo.getStatus().equals(ResponseCodes.Status.SC_OK)) {
            if (walletBalanceVo.getStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS)) {
                throw new InsufficientBalanceException();
            } else {
                throw new InvalidOperatorResponseException(walletBalanceVo.getStatus().code);
            }
        }

        String username = responseData.getUsername();
        String currency = responseData.getCurrency();
        BigDecimal balance = responseData.getBalance();

        if (!walletRequest.getTraceId().equals(walletBalanceVo.getTraceId())) { // trace id mismatch
            throw new InvalidOperatorResponseException(INVALID_RESPONSE);
        }

        if (username == null || currency == null || balance == null) { // empty response value
            throw new InvalidOperatorResponseException(INVALID_RESPONSE);
        }

        if (!walletRequest.getOperatorUsername().equals(username) || !walletRequest.getCurrencyCode().equals(currency)) {
            throw new InvalidOperatorResponseException(INVALID_RESPONSE);
        }

        return walletBalanceVo;
    }

    private String generateSignature(Object payload, String apiSecret) {
        String jsonPayload = new Gson().toJson(payload);
        return ApiSecurityUtils.getHmacSignature(jsonPayload, apiSecret);
    }
}
