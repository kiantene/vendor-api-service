package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceData;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceDto;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.repository.GameSessionRepository;
import com.nextgen.gameaggregator.vo.OperatorResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;

@Service
@Slf4j
public class WalletService {
    @Autowired
    private GameSessionRepository gameSessionRepository;

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    private WalletBalanceVo walletBalanceVo = null;

    @Autowired
    private WalletBalanceAction walletBalanceAction;

    public BigDecimal getBalance(String traceId, GameSession gameSession) throws InvalidOperatorResponseException
    {
        //TODO WAYS TO GET VENDOR CODE
        WalletBalanceDto walletBalanceDto = new WalletBalanceDto(
                gameSession.getAgentPlayerUsername(),
                traceId,
                Long.parseLong(gameSession.getAgentId().toString()),
                "PP",
                gameSession.getCurrencyCode()
        );

        //TODO READ AGENT CREDENTIALS API URL FROM DB INSTEAD OF HARDCODE
        WebClient webClient = WebClient.create("http://localhost:8087/api");

        String responses;
        responses = this.callRestApiService(walletBalanceDto, webClient);
        this.validateResponse(responses);

        if (this.walletBalanceVo.isStatus()) {
            return walletBalanceVo.getResponse().getData().getBalance();
        } else {
            throw new InvalidOperatorResponseException();
        }
    }

    private String callRestApiService(WalletBalanceDto dto, WebClient webClient)
    {
        //TODO WEB CLIENT CONSTANT
        //TODO GET SEAMLESS_WALLET_BALANCE_REQUEST
        //TODO GET SERVICE_TIMEOUT

        return webClient.post()
                .uri("/wallet/balance/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .retrieve()
                .onStatus(HttpStatus::isError, response ->
                        response.bodyToMono(String.class)
                                .doOnNext(responseBody ->
                                        logger.error("Error Operator API response from server: {}", responseBody)
                                )
                                // throw original error
                                .then(response.createException())
                )
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(10000))
                .block();
    }

    private void validateResponse(String response)
    {
        //TODO VALIDATE OPERATOR'S RESPONSES
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        this.walletBalanceVo = new Gson().fromJson(response, WalletBalanceVo.class);
    }
}
