package com.nextgen.gameaggregator.operator.wallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InternalServerException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.AuthenticationService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class WalletBaseAction {

    /*
    Error scenarios to handle:
    1. Timed out (operator did not return response within X seconds)
    2. HTTP 4xx errors
    3. HTTP 5xx errors
    4. Response invalid json format (or empty)
    5. Response missing some expected fields
    6. Response values different from request value (eg. trace Id or username)
    7. Unknown errors
     */

    private final Integer invalidResponseCode = ResponseCodes.Status.SC_INVALID_RESPONSE.code;
    protected String requestType;
    protected String endpoint;
    protected Integer timeout;

    private String toJson(Object requestData) throws InternalServerException {
        String json;

        try {
            json = new ObjectMapper().writeValueAsString(requestData);
        } catch (JsonProcessingException jsonProcessingException) {
            throw new InternalServerException(jsonProcessingException.getMessage());
        }
        return json;
    }

    private WalletBalanceVo toWalletBalanceVo(String body) throws InvalidOperatorResponseException {
        WalletBalanceVo walletBalanceVo;

        try {
            walletBalanceVo = new ObjectMapper().readValue(body, WalletBalanceVo.class);

        } catch (JsonProcessingException jsonProcessingException) {
            throw new InvalidOperatorResponseException(jsonProcessingException.getMessage(), ResponseCodes.Status.SC_INVALID_RESPONSE.code);
        }
        return walletBalanceVo;
    }

    public WalletRequest callToOperator(WalletRequest walletRequest, Object requestData)
            throws InsufficientBalanceException, InvalidOperatorResponseException, InternalServerException {

        ValidationUtils.doValidation(requestData, InternalServerException::new);

        AtomicBoolean isTimeout = new AtomicBoolean(false);
        walletRequest.setRequestType(this.requestType);

        final String apiUrl = walletRequest.getOperatorEndpoint();
        final String apiSecret = walletRequest.getApiSecret();

        String requestBody = this.toJson(requestData);
        final String signature = AuthenticationService.generateSignatureWithJson(requestBody, apiSecret);

        walletRequest.setOperatorEndpoint(apiUrl + this.endpoint);
        walletRequest.setOperatorData(requestBody);
        walletRequest.setOperatorStart(System.currentTimeMillis());

        ResponseEntity<String> response = WebClient.create(apiUrl).post()
                .uri(this.endpoint)
                .header(EndPoints.HEADER_API_KEY, walletRequest.getApiKey())
                .header(EndPoints.HEADER_SIGNATURE, signature)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .timeout(Duration.ofMillis(this.timeout))
                .onErrorResume(TimeoutException.class, e -> {
                    isTimeout.set(true);
                    return Mono.empty();
                })
                .block();

        walletRequest.setOperatorEnd(System.currentTimeMillis());

        if (isTimeout.get()) {
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code);
        }

        if (response == null) throw new InvalidOperatorResponseException(invalidResponseCode);

        HttpStatusCode httpStatusCode = response.getStatusCode();
        walletRequest.setOperatorHttpStatusCode(httpStatusCode.value());
        walletRequest.setOperatorResponse(response.getBody());

        // check is HTTP status code of the request is not 200
        if (httpStatusCode.isError()) {
            throw new InvalidOperatorResponseException(httpStatusCode.value());
        }

        WalletBalanceVo walletBalanceVo = this.toWalletBalanceVo(response.getBody());
        WalletBalanceVo.ResponseData responseData = this.validateOperatorResponse(walletRequest, walletBalanceVo);

        BigDecimal convertedBalance = new BigDecimal(responseData.getBalance().multiply(walletRequest.getToVendorRate()).stripTrailingZeros().toPlainString());
        walletRequest.setBalanceAfter(convertedBalance);
        walletRequest.setOperatorResponseStatus(walletBalanceVo.getStatus());
        walletRequest.setStatus(ResponseCodes.Status.SC_OK.code);

        return walletRequest;

    }

    private WalletBalanceVo.ResponseData validateOperatorResponse(WalletRequest request, WalletBalanceVo response)
            throws InvalidOperatorResponseException, InsufficientBalanceException {

        WalletBalanceVo.ResponseData responseData = response.getData();

        if (!response.getStatus().equals(ResponseCodes.Status.SC_OK)) {
            if (response.getStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS)) {
                throw new InsufficientBalanceException();
            } else {
                throw new InvalidOperatorResponseException(response.getStatus().code);
            }
        }

        String username = responseData.getUsername();
        String currency = responseData.getCurrency();
        BigDecimal balance = responseData.getBalance();

        if (!request.getTraceId().equals(response.getTraceId())) {
            throw new InvalidOperatorResponseException(invalidResponseCode);
        }

        if (username == null || currency == null || balance == null) {
            throw new InvalidOperatorResponseException(invalidResponseCode);
        }

        if (!request.getOperatorUsername().equals(username) || !request.getCurrencyCode().equals(currency)) {
            throw new InvalidOperatorResponseException(invalidResponseCode);
        }

        return responseData;
    }
}
