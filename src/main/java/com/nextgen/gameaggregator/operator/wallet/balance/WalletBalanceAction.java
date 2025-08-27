package com.nextgen.gameaggregator.operator.wallet.balance;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.core.common.OperatorApiCaller;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.netty.resources.ConnectionProvider;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class WalletBalanceAction {

    private final RequestService requestService;
    private final AgentApiCredentialService agentApiCredentialService;
    private final AuthenticationService authenticationService;
    private final VendorService vendorService;
    private final CurrencyConversionService currencyConversionService;
    private final ConnectionProvider operatorApiConnectionProvider;
    private final MeterRegistry meterRegistry;
    private final LogContextService logContextService;

    public WalletBalanceAction(RequestService requestService,
                               AgentApiCredentialService agentApiCredentialService,
                               AuthenticationService authenticationService,
                               VendorService vendorService,
                               CurrencyConversionService currencyConversionService,
                               ConnectionProvider operatorApiConnectionProvider,
                               MeterRegistry meterRegistry,
                               LogContextService logContextService) {

        this.requestService = requestService;
        this.agentApiCredentialService = agentApiCredentialService;
        this.authenticationService = authenticationService;
        this.vendorService = vendorService;
        this.currencyConversionService = currencyConversionService;
        this.operatorApiConnectionProvider = operatorApiConnectionProvider;
        this.meterRegistry = meterRegistry;
        this.logContextService = logContextService;
    }

    public WalletBalanceVo call(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) throws InvalidOperatorResponseException, InvalidAgentApiCredentialException, VendorCurrencyNotSupportException {

        Integer agentId = gameSession.getAgentId();
        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

        VendorCurrency vendorCurrency = vendorService.getCurrencyConversionRate(gameSession, traceId);
        BigDecimal toVendorConversionRate = vendorCurrency.getToVendorRate();

        WalletBalanceDto dto = this.newWalletBalanceDto(traceId, gameSession);
        WalletBalanceVo responseVo = null;

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            httpRequestLog.setOperatorStart(startTime);

            String jsonApiResponse = new Gson().toJson(dto);
            httpRequestLog.setOperatorData(jsonApiResponse);
            httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.WALLET_BALANCE);

        }

        // if match useStub and username prefix will skip call to stub
        if (requestService.shouldSkipStubCall(dto.getUsername())) {
            return requestService.responseOperatorSub();
        }

        ResponseEntity<String> apiResponse = null;

        try {
            OperatorApiCaller operatorApiCaller = new OperatorApiCaller(operatorApiConnectionProvider, this.meterRegistry);
            Map<String, String> headers = getHeaders(agentApiCredential.getApiKey(), signature);
            Map<String, String> extras = Map.of(
                    OperatorApiCaller.AGENT_ID, String.valueOf(agentId),
                    OperatorApiCaller.ACTION, "balance"
            );

            logContextService.logStart(apiUrl + EndPoints.WALLET_BALANCE, dto);
            apiResponse = operatorApiCaller.post(apiUrl, EndPoints.WALLET_BALANCE, headers, dto, extras);
            this.setEndTime(httpRequestLog);
            logContextService.logEnd(apiResponse);

        } catch (OperatorApiException operatorApiException) {
            this.setEndTime(httpRequestLog);
            Throwable rootCause = operatorApiException.getRootCause();
            InvalidOperatorResponseException exception = new InvalidOperatorResponseException(operatorApiException.getMessage());
            exception.setRootCause(operatorApiException.getClass().getSimpleName() + " - " + rootCause.getClass().getSimpleName());
            String responseBody = operatorApiException.getResponseBody();

            if (httpRequestLog != null) {
                if (responseBody != null && !responseBody.isEmpty()) {
                    httpRequestLog.setOperatorResponse(responseBody);
                    httpRequestLog.setOperatorHttpStatusCode(operatorApiException.getStatusCode());
                }
            }

            throw exception;
        } finally {
            logContextService.logEnd(apiResponse);
        }

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson(apiResponse.getBody(), WalletBalanceVo.class);

            if (httpRequestLog != null) {
                httpRequestLog.setOperatorResponse(apiResponse.getBody());
                httpRequestLog.setOperatorResponseStatus(responseVo.getStatus());
                Optional.ofNullable(responseVo.getData()).ifPresent(data -> httpRequestLog.setOperatorTimestamp(data.getTimestamp()));

            }

            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            RequestService.validateResponse(responseVo);

            //3. validate username and currency
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

            // 5. add conversion rate when returning the balance to vendor
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException |
                 InvalidResponseException |
                 ResponseNotMatchRequestException invalidResponseException) {

            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {

            throw new InvalidOperatorResponseException(invalidOperatorResponseException.getOperatorStatus());

        } catch (Exception exception) {

            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
        }

        return responseVo;
    }

    private Map<String, String> getHeaders(String apiKey, String signature) {
        return Map.of(
                EndPoints.HEADER_API_KEY, apiKey,
                EndPoints.HEADER_SIGNATURE, signature
        );
    }

    private WalletBalanceDto newWalletBalanceDto(String traceId, GameSession
            gameSession) {
        WalletBalanceDto walletBalanceDto = new WalletBalanceDto();
        walletBalanceDto.setTraceId(traceId);
        walletBalanceDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBalanceDto.setCurrency(gameSession.getCurrencyCode());
        walletBalanceDto.setToken(gameSession.getToken());

        return walletBalanceDto;
    }

    private void setEndTime(HttpRequestLog httpRequestLog) {
        if (httpRequestLog != null) {
            long endTime = System.currentTimeMillis();
            httpRequestLog.setOperatorEnd(endTime);
        }
    }
}
