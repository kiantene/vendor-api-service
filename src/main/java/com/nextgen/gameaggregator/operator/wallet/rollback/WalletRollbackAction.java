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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class WalletRollbackAction {
    private final RequestService requestService;
    private final AuthenticationService authenticationService;
    private final AgentApiCredentialService agentApiCredentialService;
    private final VendorService vendorService;
    private final CurrencyConversionService currencyConversionService;
    private final BetResultRetryLogService betResultRetryLogService;
    @Value("${testing.stub:false}")
    private Boolean useStub;
    @Value("${spring.profiles.active}")
    private String profilesActive;

    public WalletRollbackAction(RequestService requestService, AuthenticationService authenticationService,
                                AgentApiCredentialService agentApiCredentialService,
                                VendorService vendorService, CurrencyConversionService currencyConversionService,
                                BetResultRetryLogService betResultRetryLogService) {
        this.requestService = requestService;
        this.authenticationService = authenticationService;
        this.agentApiCredentialService = agentApiCredentialService;
        this.vendorService = vendorService;
        this.currencyConversionService = currencyConversionService;
        this.betResultRetryLogService = betResultRetryLogService;
    }

    public WalletBalanceVo
    call(String traceId, Integer agentId, GameSession gameSession, String betId, String roundId, String vendorBetId, Long rollbackTimestamp, String internalTransactionId, HttpRequestLog httpRequestLog)
            throws InvalidOperatorResponseException, InvalidAgentApiCredentialException, VendorCurrencyNotSupportException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return requestService.responseOperatorSub();
        }

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        AtomicBoolean isTimeout = new AtomicBoolean(false);
        WalletBalanceVo responseVo = new WalletBalanceVo();

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
        boolean isError = false;
        ResponseCodes.Status operatorStatus = ResponseCodes.Status.SC_UNKNOWN_ERROR;

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
                    .onErrorResume(TimeoutException.class, e -> {
                        isTimeout.set(true);
                        return Mono.error(e);
                    })
                    .block();

            long endTime = System.currentTimeMillis();
            httpRequestLog.setOperatorEnd(endTime);

            if (httpRequestLog != null) {
                if (apiResponse != null) {
                    httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());
                }
            }

            if (isTimeout.get()) {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code);
            }

            requestLogVo = requestService.createRequestLogVo(
                    EndPoints.WALLET_ROLLBACK, apiUrl, dto, apiResponse, headerMap, startTime, endTime,
                    this.getClass().getPackage().getName(), profilesActive);

            // 1. validate HTTP Response Code
            // Update remove validate to accept all http code for rollback
            //requestService.validateVendorHttpStatusResponse(apiResponse);

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

        } catch (JsonSyntaxException |
                 InvalidResponseException |
                 ResponseNotMatchRequestException invalidResponseException) {

            isError = true;
            operatorStatus = ResponseCodes.Status.SC_INVALID_RESPONSE;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {

            isError = true;
            operatorStatus = ResponseCodes.Status.checkCodeStatus(invalidOperatorResponseException.getOperatorStatus());

        } catch (Exception exception) {
            isError = true;
            operatorStatus = ResponseCodes.Status.SC_UNKNOWN_ERROR;

        } finally {
            if (isError) {
                responseVo = this.processForceSuccess(gameSession, traceId);
                if (httpRequestLog != null) {
                    betResultRetryLogService.create(httpRequestLog.getOperatorData(), gameSession.getVendorId(),
                            agentId, dto.getBetId(), dto.getRoundId(), dto.getTransactionId(), EndPoints.WALLET_ROLLBACK);
                }
            } else {
                //this is not error.
            }
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

    private WalletBalanceVo processForceSuccess(GameSession gameSession, String traceId) {

        WalletBalanceVo responseVo = new WalletBalanceVo();
        WalletBalanceVo.ResponseData data = new WalletBalanceVo.ResponseData();

        data.setBalance(BigDecimal.ZERO);
        data.setUsername(gameSession.getAgentPlayerUsername());
        data.setCurrency(gameSession.getCurrencyCode());
        data.setTimestamp(System.currentTimeMillis());

        responseVo.setTraceId(traceId);
        responseVo.setStatus(ResponseCodes.Status.SC_OK);
        responseVo.setMessage(ResponseCodes.Status.SC_OK.description);
        responseVo.setData(data);

        return responseVo;
    }
}
