package com.nextgen.gameaggregator.operator.wallet.balance;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.AgentApiCredentialService;
import com.nextgen.gameaggregator.service.AuthenticationService;
import com.nextgen.gameaggregator.service.RequestService;
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

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class WalletBalanceAction {

    @Value("${testing.stub:false}")
    private Boolean useStub;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    private RequestService requestService;
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AuthenticationService authenticationService;

    public WalletBalanceVo call(String traceId, GameSession gameSession) throws InvalidOperatorResponseException, InvalidAgentApiCredentialException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return requestService.responseOperatorSub();
        }

        Integer agentId = gameSession.getAgentId();
        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredential.getCallbackUrl();

        WalletBalanceDto dto = this.newWalletBalanceDto(traceId, gameSession);
        WalletBalanceVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(Endpoints.HEADER_SIGNATURE, signature);

        long startTime = System.currentTimeMillis();
        ResponseEntity apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(Endpoints.WALLET_BALANCE)
                .headers(requestService.setHeaders(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(Endpoints.HEADER_SIGNATURE, signature)
                .body(BodyInserters.fromValue(dto))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .retry(3)
                .block();
        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                Endpoints.WALLET_BALANCE, apiUrl, dto, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), WalletBalanceVo.class);
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            requestService.validateResponse(responseVo);

            //3. validate username and currency
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

            requestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException httpResponseStatusCodeException) {
            requestService.failResponseLog(requestLogVo, httpResponseStatusCodeException);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (JsonSyntaxException jsonSyntaxException) {
            requestService.failResponseLog(requestLogVo, jsonSyntaxException);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidResponseException invalidResponseException) {
            requestService.failResponseLog(requestLogVo, invalidResponseException);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (ResponseNotMatchRequestException responseNotMatchRequestException) {
            requestService.failResponseLog(requestLogVo, responseNotMatchRequestException);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            requestService.failResponseLog(requestLogVo, invalidOperatorResponseException);
            throw new InvalidOperatorResponseException(invalidOperatorResponseException.getOperatorStatus());

//        } catch (InvalidTokenException invalidTokenException) {
//            operatorRequestService.failResponseLog(operatorLogVo, invalidTokenException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_TOKEN.code);
//
//        } catch (InvalidSignatureException invalidSignatureException) {
//            operatorRequestService.failResponseLog(operatorLogVo, invalidSignatureException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_SIGNATURE.code);
//
//        } catch (InvalidPlayerException invalidPlayerException) {
//            operatorRequestService.failResponseLog(operatorLogVo, invalidPlayerException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_USER_NOT_EXISTS.code);
//
//        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
//            operatorRequestService.failResponseLog(operatorLogVo, disabledAgentPlayerException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
//
//        } catch (GameNotSupportedException gameNotSupportedException) {
//            operatorRequestService.failResponseLog(operatorLogVo, gameNotSupportedException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
//
//        } catch (InsufficientBalanceException insufficientBalanceException) {
//            operatorRequestService.failResponseLog(operatorLogVo, insufficientBalanceException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
//
//        } catch (InvalidRequestException invalidRequestException) {
//            operatorRequestService.failResponseLog(operatorLogVo, invalidRequestException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_REQUEST.code);
//
//        } catch (BetNotFoundException betNotFoundException) {
//            operatorRequestService.failResponseLog(operatorLogVo, betNotFoundException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_TRANSACTION_NOT_EXISTS.code);
//
//        } catch (SystemMaintenanceException systemMaintenanceException) {
//            operatorRequestService.failResponseLog(operatorLogVo, systemMaintenanceException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNDER_MAINTENANCE.code);
//
//        } catch (DuplicateTransactionException duplicateTransactionException) {
//            operatorRequestService.failResponseLog(operatorLogVo, duplicateTransactionException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_TRANSACTION_DUPLICATED.code);
//
//        } catch (DuplicateRequestException duplicateRequestException) {
//            operatorRequestService.failResponseLog(operatorLogVo, duplicateRequestException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_DUPLICATE_REQUEST.code);
//
//        } catch (InvalidCurrencyException invalidCurrencyException) {
//            operatorRequestService.failResponseLog(operatorLogVo, invalidCurrencyException.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_WRONG_CURRENCY.code);


        } catch (Exception exception) {
            requestService.failResponseLog(requestLogVo, exception);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
        }

        return responseVo;
    }

    private WalletBalanceDto newWalletBalanceDto(String traceId, com.nextgen.gameaggregator.entity.GameSession
            gameSession) {
        WalletBalanceDto walletBalanceDto = new WalletBalanceDto();
        walletBalanceDto.setTraceId(traceId);
        walletBalanceDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBalanceDto.setCurrency(gameSession.getCurrencyCode());
        walletBalanceDto.setToken(gameSession.getToken());

        return walletBalanceDto;
    }
}
