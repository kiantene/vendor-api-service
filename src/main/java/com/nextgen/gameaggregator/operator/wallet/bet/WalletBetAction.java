package com.nextgen.gameaggregator.operator.wallet.bet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class WalletBetAction {

    @Value("${testing.stub:false}")
    private Boolean useStub;
    @Value("${spring.profiles.active}")
    private String profilesActive;
    @Autowired
    RequestService requestService;
    @Autowired
    AgentApiCredentialService agentApiCredentialService;
    @Autowired
    AuthenticationService authenticationService;

    public WalletBalanceVo call(String traceId, Integer agentId, GameSession gameSession, BetResultData betResultData)
            throws InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return requestService.responseOperatorSub();
        }

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredential.getCallbackUrl();

        // 1. Generate walletBetDto
        WalletBetDto dto = this.newWalletBetDto(traceId, gameSession, betResultData);
        WalletBalanceVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(Endpoints.HEADER_SIGNATURE, signature);

        long startTime = System.currentTimeMillis();
        ResponseEntity apiResponse = WebClient.create(agentApiCredential.getCallbackUrl())
                .post()
                .uri(Endpoints.WALLET_BET)
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
                .block();
        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                Endpoints.WALLET_BET, apiUrl, dto, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            System.out.println("apiResponse = " + apiResponse);
            System.out.println("dto = " + dto);

            //2. validate operator response
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), WalletBalanceVo.class);
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            requestService.validateResponse(responseVo);

            //3. validate username and currency
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

            BigDecimal balance = responseVo.getData().getBalance();
            //TODO to be discuss whether should system pre handle negative if
            boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
            if (isNegativeBalance) {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            }

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

        } catch (Exception exception) {
            requestService.failResponseLog(requestLogVo, exception);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
        }
        return responseVo;
    }

    private WalletBetDto newWalletBetDto(String traceId, GameSession gameSession, BetResultData betResultData) {
        BigDecimal amount = new BigDecimal(betResultData.getBetAmount().stripTrailingZeros().toPlainString());

        WalletBetDto walletBetDto = new WalletBetDto();
        walletBetDto.setTraceId(traceId);
        walletBetDto.setTransactionId(traceId);
        walletBetDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBetDto.setCurrency(gameSession.getCurrencyCode());
        walletBetDto.setToken(gameSession.getToken());
        //UPDATE PG : USE VENDORBETID
        walletBetDto.setExternalTransactionId(betResultData.getVendorBetId());
        walletBetDto.setAmount(amount);
        walletBetDto.setGameCode(gameSession.getGameCode());
        walletBetDto.setRoundId(betResultData.getRoundId());
        walletBetDto.setTimestamp(betResultData.getVendorBetTime());

        return walletBetDto;
    }
}
