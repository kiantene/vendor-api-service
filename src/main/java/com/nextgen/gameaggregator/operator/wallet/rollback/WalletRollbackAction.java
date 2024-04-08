package com.nextgen.gameaggregator.operator.wallet.rollback;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.RequestLogVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class WalletRollbackAction {
    @Autowired
    RequestService requestService;
    @Value("${testing.stub:false}")
    private Boolean useStub;
    @Value("${spring.profiles.active}")
    private String profilesActive;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private CurrencyConversionService currencyConversionService;

    public WalletBalanceVo
    call(String traceId, Integer agentId, GameSession gameSession, String betId, String roundId, String vendorBetId, Long rollbackTimestamp, String internalTransactionId, HttpRequestLog httpRequestLog)
            throws InvalidOperatorResponseException, InvalidAgentApiCredentialException, VendorCurrencyNotSupportException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return requestService.responseOperatorSub();
        }

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo;

        VendorCurrency vendorCurrency = vendorService.getCurrencyConversionRate(gameSession, traceId);
        BigDecimal toVendorConversionRate = vendorCurrency.getToVendorRate();

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);
        WalletRollbackDto dto = this.newWalletRollbackDto(traceId, betId, vendorBetId, roundId, gameSession, rollbackTimestamp, internalTransactionId);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);
        headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            httpRequestLog.setOperatorStart(startTime);

            String jsonApiResponse = new Gson().toJson(dto);
            httpRequestLog.setOperatorData(jsonApiResponse);
            httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.WALLET_ROLLBACK);

        }

        RequestLogVo requestLogVo = null;

        try {
            ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.WALLET_ROLLBACK)
                    .header(EndPoints.HEADER_SIGNATURE, signature)
                    .header(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                    .toEntity(String.class)
                    .retry(3)
                    .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                    .block();

            long endTime = System.currentTimeMillis();
            if (httpRequestLog != null) {
                if (apiResponse != null) {
                    httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());

                }
                httpRequestLog.setOperatorEnd(endTime);
            }

            requestLogVo = requestService.createRequestLogVo(
                    EndPoints.WALLET_ROLLBACK, apiUrl, dto, apiResponse, headerMap, startTime, endTime,
                    this.getClass().getPackage().getName(), profilesActive);

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), WalletBalanceVo.class);
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            RequestService.validateResponse(responseVo);

            if (httpRequestLog != null) {
                httpRequestLog.setOperatorResponse(apiResponse.getBody());
                httpRequestLog.setOperatorResponseStatus(responseVo.getStatus());
                Optional.ofNullable(responseVo.getData()).ifPresent(data -> httpRequestLog.setOperatorTimestamp(data.getTimestamp()));

            }

            //3. validate username and currency
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

            // 5. add conversion rate when returning the balance to vendor
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

            //RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException |
                 InvalidResponseException |
                 ResponseNotMatchRequestException invalidResponseException) {

            //RequestService.failResponseLog(requestLogVo, invalidResponseException);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //RequestService.failResponseLog(requestLogVo, invalidOperatorResponseException);
            throw new InvalidOperatorResponseException(invalidOperatorResponseException.getOperatorStatus());

        } catch (Exception exception) {
            long endTime = System.currentTimeMillis();
            Integer defaultOperatorErrorResponse = ResponseCodes.Status.SC_UNKNOWN_ERROR.code;

            requestLogVo = requestService.createRequestLogVo(
                    EndPoints.WALLET_ROLLBACK, apiUrl, dto, null, headerMap, startTime, endTime,
                    this.getClass().getPackage().getName(), profilesActive);

            if (exception.getMessage().contains("java.util.concurrent.TimeoutException")) {
                defaultOperatorErrorResponse = ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code;
            }

            throw new InvalidOperatorResponseException(defaultOperatorErrorResponse);
        }

        return responseVo;
    }

    private WalletRollbackDto newWalletRollbackDto(
            String traceId, String betId, String vendorBetId, String roundId, GameSession gameSession, Long rollbackTimestamp, String internalTransactionId) {
        WalletRollbackDto walletRollbackDto = new WalletRollbackDto();
        walletRollbackDto.setTraceId(traceId);
        walletRollbackDto.setTransactionId(internalTransactionId);
        walletRollbackDto.setBetId(betId);
        walletRollbackDto.setExternalTransactionId(vendorBetId);
        walletRollbackDto.setRoundId(roundId);
        walletRollbackDto.setGameCode(gameSession.getGameCode());
        walletRollbackDto.setUsername(gameSession.getAgentPlayerUsername());
        walletRollbackDto.setCurrency(gameSession.getCurrencyCode());
        walletRollbackDto.setTimestamp(rollbackTimestamp);

        return walletRollbackDto;
    }
}
