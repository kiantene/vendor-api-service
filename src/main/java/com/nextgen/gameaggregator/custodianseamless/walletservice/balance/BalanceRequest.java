package com.nextgen.gameaggregator.custodianseamless.walletservice.balance;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import com.nextgen.gameaggregator.custodianseamless.exception.InvalidWalletServiceResponseException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceAccessKeyNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceTimeoutException;
import com.nextgen.gameaggregator.custodianseamless.operator.balance.BalanceData;
import com.nextgen.gameaggregator.custodianseamless.service.TransferService;
import com.nextgen.gameaggregator.custodianseamless.service.WalletRequestValidationService;
import com.nextgen.gameaggregator.custodianseamless.walletservice.dto.WalletServiceBalanceDto;
import com.nextgen.gameaggregator.custodianseamless.walletservice.vo.BalanceVo;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.wallet.AccessKey;
import com.nextgen.gameaggregator.exception.InvalidResponseException;
import com.nextgen.gameaggregator.logging.TransferWalletRequestLog;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.AuthenticationService;
import com.nextgen.gameaggregator.service.LoggingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class BalanceRequest {

    @Value("${walletservice.host}")
    private String walletServiceUrl;

    @Value("${walletservice.timeout:1000}")
    public Integer timeout;

    private final AuthenticationService authenticationService;
    private final TransferService transferService;
    private final LoggingService loggingService;

    public BalanceRequest(AuthenticationService authenticationService,
                          TransferService transferService,
                          LoggingService loggingService) {

        this.authenticationService = authenticationService;
        this.transferService = transferService;
        this.loggingService = loggingService;
    }

    public BalanceData call(String traceId, AgentPlayer agentPlayer, Currency currency, TransferWalletRequestLog transferWalletRequestLog) throws
            WalletServiceAccessKeyNotFoundException, InvalidWalletServiceResponseException, WalletServiceTimeoutException {

        BalanceVo responseVo;
        BalanceData balanceData = new BalanceData(agentPlayer, currency);
        loggingService.logStart();
        AccessKey accessKey = transferService.getWalletServiceAccessKey();
        loggingService.logProcessTime("balance ｜ transferService.getWalletServiceAccessKey", traceId);
        String apiUrl = walletServiceUrl;

        WalletServiceBalanceDto dto = new WalletServiceBalanceDto(traceId, agentPlayer, currency);
        String signature = authenticationService.generateSignature(dto, accessKey.getApiSecret());

        transferWalletRequestLog.setWalletStart(System.currentTimeMillis());
        transferWalletRequestLog.setWalletData(new Gson().toJson(dto));

        AtomicBoolean isTimeout = new AtomicBoolean(false);
        ResponseEntity<String> response = null;

        try {
            response = WebClient.create(apiUrl).post().uri(WalletServiceEndpoints.WALLET_BALANCE)
                    .header(WalletServiceEndpoints.HEADER_API_KEY, accessKey.getApiKey())
                    .header(WalletServiceEndpoints.HEADER_SIGNATURE, signature)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> Mono.empty())
                    .toEntity(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .onErrorResume(TimeoutException.class, e -> {
                        isTimeout.set(true);
                        return Mono.empty();
                    })
                    .onErrorResume(WebClientRequestException.class, e -> {
                        log.error("TraceId {} Failed wallet service call to {}: {}, ",
                                traceId, apiUrl + WalletServiceEndpoints.WALLET_BALANCE, e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error wallet service call to " + apiUrl + WalletServiceEndpoints.WALLET_BALANCE
                                        + ", Exception " + e.getMessage()));
                    })
                    .block();

            Long timestamp = System.currentTimeMillis();
            transferWalletRequestLog.setWalletEnd(timestamp);
            responseVo = this.validateResponse(response, isTimeout, transferWalletRequestLog);
            balanceData.setAmount(responseVo.getData().getBalance());
            balanceData.setTimestamp(timestamp);
            transferWalletRequestLog.setAmount(balanceData.getAmount());

        } catch (InvalidResponseException invalidResponseException) {
            throw new InvalidWalletServiceResponseException(invalidResponseException.getMessage(), ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (Exception exception) {
            transferWalletRequestLog.setException(exception.getClass().getSimpleName());
            transferWalletRequestLog.setExceptionMessage(exception.getMessage());

            throw exception;
        } finally {
            if (transferWalletRequestLog.getWalletEnd() == null) {
                Long timestamp = System.currentTimeMillis();
                transferWalletRequestLog.setWalletEnd(timestamp);
            }

            if (response != null) {
                HttpStatusCode httpStatusCode = response.getStatusCode();
                transferWalletRequestLog.setWalletHttpStatusCode(httpStatusCode.value());
            }
        }

        return balanceData;
    }

    private BalanceVo validateResponse(ResponseEntity<String> response, AtomicBoolean isTimeout, TransferWalletRequestLog transferWalletRequestLog)
            throws InvalidWalletServiceResponseException, JsonSyntaxException, WalletServiceTimeoutException, InvalidResponseException {

        final Integer INVALID_RESPONSE = ResponseCodes.Status.SC_INVALID_RESPONSE.code;

        // check if it is timed out
        if (isTimeout.get()) throw new WalletServiceTimeoutException(ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code);

        // check if response is empty
        if (response == null) throw new InvalidWalletServiceResponseException(INVALID_RESPONSE);

        HttpStatusCode httpStatusCode = response.getStatusCode();
        transferWalletRequestLog.setWalletHttpStatusCode(httpStatusCode.value());
        transferWalletRequestLog.setWalletResponse(response.getBody());

        // check is HTTP status code of the request is not 200
        if (httpStatusCode.isError()) {
            throw new InvalidWalletServiceResponseException(httpStatusCode.value());
        }

        // convert response to VO
        BalanceVo balanceVo = new Gson().fromJson(response.getBody(), BalanceVo.class);
        WalletRequestValidationService.validateResponse(balanceVo);
        BalanceVo.ResponseData responseData = balanceVo.getData();

        // check if wallet response status is empty
        if (balanceVo.getStatus() == null) throw new InvalidWalletServiceResponseException(INVALID_RESPONSE);

        transferWalletRequestLog.setWalletResponseStatus(balanceVo.getStatus());

        // check if wallet response status is SC_OK
        if (!balanceVo.getStatus().equals(ResponseCodes.Status.SC_OK)) {
            throw new InvalidWalletServiceResponseException(balanceVo.getStatus().code);
        }

        // validate username and tokenId
        String username = responseData.getUsername();
        Integer tokenId = responseData.getTokenId();
        BigDecimal balance = responseData.getBalance();

        if (!transferWalletRequestLog.getTraceId().equals(balanceVo.getTraceId())) { // trace id mismatch
            throw new InvalidWalletServiceResponseException(ResponseCodes.Status.SC_TRACE_ID_MISMATCHED.code);
        }

        if (username == null || tokenId == null || balance == null) { // empty response value
            throw new InvalidWalletServiceResponseException(INVALID_RESPONSE);
        }

        if (!transferWalletRequestLog.getUsername().equals(username)) {
            throw new InvalidWalletServiceResponseException(ResponseCodes.Status.SC_USERNAME_MISMATCHED.code);
        }

        return balanceVo;
    }
}
