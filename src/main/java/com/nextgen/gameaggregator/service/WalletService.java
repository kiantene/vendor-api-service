package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceDto;
import com.nextgen.gameaggregator.repository.GameSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;

import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WalletService {
    @Autowired
    private GameSessionRepository gameSessionRepository;

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    public BigDecimal getBalance(String traceId, GameSession gameSession) throws IOException, InterruptedException {

        WalletBalanceDto walletBalanceDto = new WalletBalanceDto(
                gameSession.getAgentPlayerUsername(),
                traceId,
                Long.parseLong(gameSession.getAgentId().toString()),
                "PP",
                gameSession.getCurrencyCode()
        );

        WebClient webClient = WebClient.create("http://poc.cd-gmagg.tk/api");

        String responses;

        responses = this.callRestApiService(walletBalanceDto, webClient, gameSession, traceId);

        return new BigDecimal("1000");
    }

    private String callRestApiService(WalletBalanceDto dto, WebClient webClient, GameSession gameSession,
                                      String traceId) throws IOException, InterruptedException {

        //TODO WEB CLIENT CONSTANT
        //TODO GET SEAMLESS_WALLET_BALANCE_REQUEST
        //TODO GET SERVICE_TIMEOUT

        String url = "http://poc.cd-gmagg.tk/api//wallet/balance/";

        ReactorClientHttpConnector connector = new ReactorClientHttpConnector();
        WebClient client = WebClient.builder().baseUrl(url).clientConnector(new ReactorClientHttpConnector(HttpClient.newConnection().compress(true))).build();

        return client.post()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
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
                .timeout(Duration.ofSeconds(30))
                .block();

//        return webClient.post()
//                .uri("/wallet/balance/")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .body(BodyInserters.fromValue(dto))
//                .retrieve()
//                .onStatus(HttpStatus::isError, response ->
//                        response.bodyToMono(String.class)
//                                .doOnNext(responseBody ->
//                                        logger.error("Error Operator API response from server: {}", responseBody)
//                                )
//                                // throw original error
//                                .then(response.createException())
//                )
//                .bodyToMono(String.class)
//                .timeout(Duration.ofMillis(10000))
//                .block();
    }
}
