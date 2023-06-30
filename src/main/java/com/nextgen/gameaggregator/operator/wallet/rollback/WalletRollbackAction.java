package com.nextgen.gameaggregator.operator.wallet.rollback;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
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

    public WalletBalanceVo call(String traceId, Integer agentId, GameSession gameSession, String betId, String roundId, String vendorBetId, Long rollbackTimestamp, String internalTransactionId)
            throws InvalidOperatorResponseException, InvalidAgentApiCredentialException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return requestService.responseOperatorSub();
        }

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo;

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredential.getCallbackUrl();
        WalletRollbackDto dto = this.newWalletRollbackDto(traceId, betId, vendorBetId, roundId, gameSession, rollbackTimestamp, internalTransactionId);
        log.info("[" + apiUrl + EndPoints.WALLET_ROLLBACK + "] Request: " + dto);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.WALLET_ROLLBACK)
                .header(EndPoints.HEADER_SIGNATURE, signature)
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

        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.WALLET_BET_RESULT, apiUrl, dto, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            log.info("[" + apiUrl + EndPoints.WALLET_ROLLBACK + "] Response: " + apiResponse);

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), WalletBalanceVo.class);
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            RequestService.validateResponse(responseVo);

            //3. validate username and currency
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException |
                 InvalidResponseException |
                 ResponseNotMatchRequestException invalidResponseException) {

            RequestService.failResponseLog(requestLogVo, invalidResponseException);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            RequestService.failResponseLog(requestLogVo, invalidOperatorResponseException);
            throw new InvalidOperatorResponseException(invalidOperatorResponseException.getOperatorStatus());

        } catch (Exception exception) {
            RequestService.failResponseLog(requestLogVo, exception);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
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
