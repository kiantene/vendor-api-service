package com.nextgen.gameaggregator.operator.wallet.rollback;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.AgentApiCredentialService;
import com.nextgen.gameaggregator.service.AuthenticationService;
import com.nextgen.gameaggregator.service.OperatorRequestService;
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
public class WalletRollbackAction {
    @Value("${testing.stub:false}")
    private Boolean useStub;
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    RequestService requestService;
    @Autowired
    private OperatorRequestService operatorRequestService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;

    public WalletBalanceVo call(String traceId, Integer agentId, GameSession gameSession, String betId, String roundId, String externalTransactionId)
            throws InvalidOperatorResponseException, InvalidAgentApiCredentialException {
        // Call stub function instead if config file set to use stub
        if (useStub) {
            return operatorRequestService.responseOperatorSub();
        }

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredential.getCallbackUrl();
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        WalletRollbackDto dto = this.newWalletRollbackDto(traceId, betId, externalTransactionId, roundId, gameSession);
        WalletBalanceVo responseVo = null;

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(Endpoints.HEADER_SIGNATURE, signature);

        long startTime = System.currentTimeMillis();
        ResponseEntity apiResponse = WebClient.create(agentApiCredential.getCallbackUrl())
                .post()
                .uri(Endpoints.WALLET_ROLLBACK)
                .headers(h -> h.addAll(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                Endpoints.WALLET_BET_RESULT, apiUrl, dto, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), WalletBalanceVo.class);
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            requestService.validateResponse(responseVo);

            System.out.println("apiResponse = " + apiResponse);
            System.out.println("dto = " + dto);

            //3. validate username and currency
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

//            BigDecimal balance = responseVo.getData().getBalance();
//            //TODO to be discuss whether should system pre handle negative if
//            boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
//            if (isNegativeBalance) {
//                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
//            }

            requestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException |
                 InvalidResponseException |
                 ResponseNotMatchRequestException invalidResponseException) {

            requestService.failResponseLog(requestLogVo, invalidResponseException);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            requestService.failResponseLog(requestLogVo, invalidOperatorResponseException);
            throw new InvalidOperatorResponseException(invalidOperatorResponseException.getOperatorStatus());

        } catch (Exception exception) {
            requestService.failResponseLog(requestLogVo, exception);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
        }
        return responseVo;
    }

    private WalletRollbackDto newWalletRollbackDto(
            String traceId, String betId, String externalTransactionId, String roundId, GameSession gameSession) {
        WalletRollbackDto walletRollbackDto = new WalletRollbackDto();
        walletRollbackDto.setTraceId(traceId);
        walletRollbackDto.setTransactionId(traceId);
        walletRollbackDto.setBetId(betId);
        walletRollbackDto.setExternalTransactionId(externalTransactionId);
        walletRollbackDto.setRoundId(roundId);
        walletRollbackDto.setGameCode(gameSession.getGameCode());
        walletRollbackDto.setUsername(gameSession.getAgentPlayerUsername());
        walletRollbackDto.setCurrency(gameSession.getCurrencyCode());
        walletRollbackDto.setTimestamp(System.currentTimeMillis());

        return walletRollbackDto;
    }
}
