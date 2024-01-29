package com.nextgen.gameaggregator.custodianseamless.walletservice.withdraw;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionStatus;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import com.nextgen.gameaggregator.custodianseamless.exception.InvalidWalletServiceResponseException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceAccessKeyNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceTimeoutException;
import com.nextgen.gameaggregator.custodianseamless.operator.dto.TransferWalletRequestLog;
import com.nextgen.gameaggregator.custodianseamless.service.TransferService;
import com.nextgen.gameaggregator.custodianseamless.service.WalletRequestValidationService;
import com.nextgen.gameaggregator.custodianseamless.walletservice.dto.WalletServiceTransferDto;
import com.nextgen.gameaggregator.custodianseamless.walletservice.vo.BalanceBeforeAfterVo;
import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import com.nextgen.gameaggregator.entity.wallet.AccessKey;
import com.nextgen.gameaggregator.exception.HttpResponseStatusCodeException;
import com.nextgen.gameaggregator.exception.InvalidResponseException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class WithdrawRequest {

    @Value("${walletservice.host}")
    private String walletServiceUrl;

    @Value("${walletservice.timeout:1000}")
    public Integer timeout;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    WalletRequestValidationService walletRequestValidationService;

    @Autowired
    TransferService transferService;

    public RawTransferHistory call(String traceId, RawTransferHistory rawTransferHistory, TransferWalletRequestLog transferWalletRequestLog) throws
            WalletServiceAccessKeyNotFoundException, InvalidWalletServiceResponseException, WalletServiceTimeoutException {

        //Default set transaction status = fail
        rawTransferHistory.setTransactionStatus(TransactionStatus.FAIL.status);

        BalanceBeforeAfterVo responseVo;

        AccessKey accessKey = transferService.getWalletServiceAccessKey();
        String apiUrl = walletServiceUrl;

        WalletServiceTransferDto dto = new WalletServiceTransferDto(traceId, rawTransferHistory);

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        String signature = authenticationService.generateSignature(dto, accessKey.getApiSecret());
        headerMap.add(WalletServiceEndpoints.HEADER_SIGNATURE, signature);
        headerMap.add(WalletServiceEndpoints.HEADER_API_KEY, accessKey.getApiKey());

        transferWalletRequestLog.setWalletServiceStart(System.currentTimeMillis());
        transferWalletRequestLog.setWalletServiceHeader(headerMap);
        transferWalletRequestLog.setWalletServiceData(dto);
        try {
            ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(WalletServiceEndpoints.WALLET_WITHDRAW)
                    .header(WalletServiceEndpoints.HEADER_SIGNATURE, signature)
                    .header(WalletServiceEndpoints.HEADER_API_KEY, accessKey.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                    .toEntity(String.class)
                    .onErrorResume(WebClientRequestException.class, e -> {
                        log.error("TraceId {} Failed wallet service call to {}: {}, ",
                                traceId, apiUrl + WalletServiceEndpoints.WALLET_WITHDRAW, e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error wallet service call to " + apiUrl + WalletServiceEndpoints.WALLET_WITHDRAW
                                        + ", Exception " + e.getMessage()));
                    })
                    .retry(1)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            transferWalletRequestLog.setWalletServiceEnd(System.currentTimeMillis());

            if (apiResponse != null) {
                transferWalletRequestLog.setWalletServiceHttpStatusCode(apiResponse.getStatusCode().value());
                transferWalletRequestLog.setWalletServiceResponse(apiResponse.getBody());
            }


            // 1. validate HTTP Response Code
            WalletRequestValidationService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate wallet service response
            responseVo = new Gson().fromJson(apiResponse.getBody(), BalanceBeforeAfterVo.class);

            // 4. wallet service response object validation
            Optional.ofNullable(responseVo).orElseThrow(InvalidWalletServiceResponseException::new);
            WalletRequestValidationService.validateResponse(responseVo);

            if (responseVo.getStatus() != null) {
                transferWalletRequestLog.setWalletServiceResponseStatus(responseVo.getStatus());
                rawTransferHistory.setErrorCode(responseVo.getStatus().code);
            }

            // 4. validate wallet response fail status
            if (responseVo.getStatus().equals(ResponseCodes.Status.SC_OK)) {
                rawTransferHistory.setResultTime(responseVo.getData().getCompletedAt());
                rawTransferHistory.setBalanceAfter(responseVo.getData().getBalanceAfter());
                rawTransferHistory.setBalanceBefore(responseVo.getData().getBalanceBefore());
                rawTransferHistory.setTransactionId(responseVo.getData().getTransactionId());
                rawTransferHistory.setTransactionStatus(TransactionStatus.SUCCESS.status);

            } else if (responseVo.getStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS)) {
                //TODO : need wallet service done the changes
//                rawTransferHistory.setResultTime(responseVo.getData().getCompletedAt());
//                rawTransferHistory.setBalanceAfter(responseVo.getData().getBalanceAfter());
//                rawTransferHistory.setBalanceBefore(responseVo.getData().getBalanceBefore());
                rawTransferHistory.setTransactionStatus(TransactionStatus.FAIL.status);

            } else if (responseVo.getStatus().equals(ResponseCodes.Status.SC_USER_NOT_EXISTS)) {
                //TODO : need wallet service done the changes
//                rawTransferHistory.setResultTime(responseVo.getData().getCompletedAt());
                rawTransferHistory.setBalanceAfter(BigDecimal.ZERO);
                rawTransferHistory.setBalanceBefore(BigDecimal.ZERO);
//                rawTransferHistory.setTransactionId(responseVo.getData().getTransactionId());
                rawTransferHistory.setTransactionStatus(TransactionStatus.FAIL.status);

                rawTransferHistory.setResultTime(System.currentTimeMillis());
            } else {
                throw new InvalidResponseException("Invalid Response Code :" + responseVo.getStatus());
            }

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException | InvalidResponseException
                invalidResponseException) {
            throw new InvalidWalletServiceResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (Exception exception) {
            exception.printStackTrace();
            if (exception.getMessage().contains("java.util.concurrent.TimeoutException")) {
                transferWalletRequestLog.setWalletServiceEnd(System.currentTimeMillis());
                throw new WalletServiceTimeoutException(ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code);
            } else {
                exception.printStackTrace();
                throw new InvalidWalletServiceResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
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


}
