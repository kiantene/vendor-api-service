package com.nextgen.gameaggregator.custodianseamless.walletservice.deposit;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionStatus;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionType;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import com.nextgen.gameaggregator.custodianseamless.exception.InvalidWalletServiceResponseException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceAccessKeyNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceTimeoutException;
import com.nextgen.gameaggregator.custodianseamless.service.TransferService;
import com.nextgen.gameaggregator.custodianseamless.service.WalletRequestValidationService;
import com.nextgen.gameaggregator.custodianseamless.walletservice.dto.WalletServiceTransferDto;
import com.nextgen.gameaggregator.custodianseamless.walletservice.vo.BalanceBeforeAfterVo;
import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import com.nextgen.gameaggregator.entity.wallet.AccessKey;
import com.nextgen.gameaggregator.exception.InvalidResponseException;
import com.nextgen.gameaggregator.logging.TransferWalletRequestLog;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.AuthenticationService;
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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class DepositRequest {

    @Value("${walletservice.host}")
    private String walletServiceUrl;

    @Value("${walletservice.timeout:1000}")
    public Integer timeout;

    private final AuthenticationService authenticationService;
    private final TransferService transferService;

    public DepositRequest(AuthenticationService authenticationService, TransferService transferService) {

        this.authenticationService = authenticationService;
        this.transferService = transferService;
    }

    public RawTransferHistory call(String traceId, RawTransferHistory rawTransferHistory, TransferWalletRequestLog transferWalletRequestLog) throws
            WalletServiceAccessKeyNotFoundException, InvalidWalletServiceResponseException, WalletServiceTimeoutException {

        //Default set transaction status = fail
        rawTransferHistory.setTransactionStatus(TransactionStatus.FAIL.status);

        BalanceBeforeAfterVo responseVo;

        AccessKey accessKey = transferService.getWalletServiceAccessKey();
        String apiUrl = walletServiceUrl;

        WalletServiceTransferDto dto = new WalletServiceTransferDto(traceId, rawTransferHistory, TransactionType.DEPOSIT.status);

        String signature = authenticationService.generateSignature(dto, accessKey.getApiSecret());

        transferWalletRequestLog.setWalletStart(System.currentTimeMillis());
        transferWalletRequestLog.setWalletData(new Gson().toJson(dto));

        AtomicBoolean isTimeout = new AtomicBoolean(false);
        ResponseEntity<String> response = null;

        try {
            response = WebClient.create(apiUrl).post().uri(WalletServiceEndpoints.WALLET_DEPOSIT)
                    .header(WalletServiceEndpoints.HEADER_SIGNATURE, signature)
                    .header(WalletServiceEndpoints.HEADER_API_KEY, accessKey.getApiKey())
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
                                traceId, apiUrl + WalletServiceEndpoints.WALLET_DEPOSIT, e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error wallet service call to " + apiUrl + WalletServiceEndpoints.WALLET_DEPOSIT
                                        + ", Exception " + e.getMessage()));
                    })
                    .block();

            Long timestamp = System.currentTimeMillis();
            transferWalletRequestLog.setWalletEnd(timestamp);
            responseVo = this.validateResponse(response, isTimeout, transferWalletRequestLog);
            // 4. wallet service response object validation
            WalletRequestValidationService.validateResponse(responseVo);

            if (responseVo.getStatus() != null) {
                rawTransferHistory.setErrorCode(responseVo.getStatus().code);
            }

            // 5. validate wallet response fail status
            rawTransferHistory = transferService.mapWalletServiceResponse(rawTransferHistory, responseVo, TransactionType.DEPOSIT.status);

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

        return rawTransferHistory;

        /**
         *
         * {
         *     "traceId": "{{traceId}}",
         *     "referenceId": "{{referenceId}}",
         *     "username": "haihung",  // actual username
         *     "playerId": 102,
         *     "entityId": 3,
         *     "walletType": 1, // 1 - Main, 2 - Promo
         *     "transactionType": 4, // 1=DEPOSIT, 2=WITHDRAWAL, 3=WITHDRAWAL REFUND, 4=WITHDRAWAL CLAWBACK, 5=BET, 6=BET WIN, 7=BET REFUND, 8=BET ADJUSTMENT, 9=MANUAL ADJUSTMENT, 10=REBATE, 11=LOYALTY, 12=AFFILIATECLAWBACK
         *     "tokenId": 2,
         *     "amount": 10000000,
         *     "timestamp": 99999,
         * }
         **
         */
    }

    private BalanceBeforeAfterVo validateResponse(ResponseEntity<String> response, AtomicBoolean isTimeout, TransferWalletRequestLog transferWalletRequestLog)
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
        BalanceBeforeAfterVo balanceVo = new Gson().fromJson(response.getBody(), BalanceBeforeAfterVo.class);
        WalletRequestValidationService.validateResponse(balanceVo);
        BalanceBeforeAfterVo.ResponseData responseData = balanceVo.getData();

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

        if (!transferWalletRequestLog.getTraceId().equals(balanceVo.getTraceId())) { // trace id mismatch
            throw new InvalidWalletServiceResponseException(ResponseCodes.Status.SC_TRACE_ID_MISMATCHED.code);
        }

        if (username == null || tokenId == null) { // empty response value
            throw new InvalidWalletServiceResponseException(INVALID_RESPONSE);
        }

        if (!transferWalletRequestLog.getUsername().equals(username)) {
            throw new InvalidWalletServiceResponseException(ResponseCodes.Status.SC_USERNAME_MISMATCHED.code);
        }

        return balanceVo;
    }
}
