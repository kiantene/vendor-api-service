package com.nextgen.gameaggregator.operator.wallet.rollback;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.AgentApiCredentialService;
import com.nextgen.gameaggregator.service.AuthenticationService;
import com.nextgen.gameaggregator.service.OperatorRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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

        WalletRollbackDto dto = this.newWalletRollbackDto(traceId, betId, externalTransactionId, roundId, gameSession);
        WalletBalanceVo responseVo = null;
        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());

        String responseString = WebClient.create(apiUrl)
                .post()
                .uri(Endpoints.WALLET_ROLLBACK)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(Endpoints.HEADER_SIGNATURE, signature)
                .body(BodyInserters.fromValue(dto))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT)) // TODO: timeout constant
                .block();

        try {
            responseVo = new Gson().fromJson(responseString, WalletBalanceVo.class);
            // throw exception if response is null
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));

            operatorRequestService.validateResponse(responseVo);

            if (!responseVo.getStatus().equals(ResponseCodes.Status.SC_OK)) {
                throw new InvalidOperatorResponseException(responseVo.toString(), responseVo.getStatus().code);
            } else {
                operatorRequestService.operatorResponseLogging(true, Endpoints.WALLET_BALANCE, agentApiCredential.getCallbackUrl(), dto, responseString, profilesActive);
            }

        } catch (JsonSyntaxException | InvalidOperatorResponseException exception) {
            operatorRequestService.operatorResponseLogging(false, Endpoints.WALLET_BALANCE, agentApiCredential.getCallbackUrl(), dto, responseString, profilesActive);
            new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
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
