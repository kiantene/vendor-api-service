package com.nextgen.gameaggregator.operator.wallet.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.core.common.OperatorApiCaller;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
    private final OperatorApiCaller operatorApiCaller;


    public WalletBalanceAction(RequestService requestService,
                               AgentApiCredentialService agentApiCredentialService,
                               AuthenticationService authenticationService,
                               VendorService vendorService,
                               CurrencyConversionService currencyConversionService,
                               OperatorApiCaller operatorApiCaller) {

        this.requestService = requestService;
        this.agentApiCredentialService = agentApiCredentialService;
        this.authenticationService = authenticationService;
        this.vendorService = vendorService;
        this.currencyConversionService = currencyConversionService;
        this.operatorApiCaller = operatorApiCaller;
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

        ResponseEntity<String> apiResponse;
        long endTime;

        try {
            ClientBalanceResponse clientBalanceResponse = operatorApiCaller.post(apiUrl, EndPoints.WALLET_BALANCE, Map.of(
                    EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey(),
                    EndPoints.HEADER_SIGNATURE, signature
            ), dto);

            String jsonResponseBody = new ObjectMapper().writeValueAsString(clientBalanceResponse);

            apiResponse = ResponseEntity.ok(jsonResponseBody);
            endTime = System.currentTimeMillis();

            if (httpRequestLog != null) {
                httpRequestLog.setOperatorResponse(jsonResponseBody);
                httpRequestLog.setOperatorEnd(endTime);
            }

        } catch (OperatorApiException operatorApiException) {
            Throwable rootCause = operatorApiException.getRootCause();
            log.error("Exception = {}, rootCause = {}, error = {}", operatorApiException.getMessage(), rootCause.getClass().getSimpleName(), rootCause.getMessage());
            throw new InvalidOperatorResponseException(rootCause.getClass().getSimpleName() + " : " + rootCause.getMessage());

        } catch (JsonProcessingException e) {
            throw new InvalidOperatorResponseException("cannot convert balance response object to string");
        }

//        ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
//                .post()
//                .uri(EndPoints.WALLET_BALANCE)
//                .header(EndPoints.HEADER_SIGNATURE, signature)
//                .header(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey())
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .body(BodyInserters.fromValue(dto))
//                .retrieve()
//                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
//                .toEntity(String.class)
//                .retry(3)
//                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
//                .retry(3)
//                .block();
//
//        long endTime = System.currentTimeMillis();
//        if (httpRequestLog != null) {
//            if (apiResponse != null) {
//                httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());
//
//            }
//            httpRequestLog.setOperatorEnd(endTime);
//        }
//

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

            //RequestService.successResponseLog(requestLogVo);

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

    private WalletBalanceDto newWalletBalanceDto(String traceId, GameSession
            gameSession) {
        WalletBalanceDto walletBalanceDto = new WalletBalanceDto();
        walletBalanceDto.setTraceId(traceId);
        walletBalanceDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBalanceDto.setCurrency(gameSession.getCurrencyCode());
        walletBalanceDto.setToken(gameSession.getToken());

        return walletBalanceDto;
    }
}
