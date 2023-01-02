package com.nextgen.gameaggregator.operator.wallet.bet;

import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@Slf4j
public class WalletBetAction {
    public WalletBalanceVo call(String callbackUrl, String signature, WalletBetDto dto) {
        log.info(dto.toString());

        WalletBalanceVo responseVo = WebClient.create(callbackUrl)
                .post()
                .uri(Endpoints.WALLET_BET)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(Endpoints.HEADER_SIGNATURE, signature)
                .body(BodyInserters.fromValue(dto))
                .retrieve()
                // TODO: proper error handling
                .onStatus(HttpStatus::isError, response ->
                        response.bodyToMono(String.class)
                                .doOnNext(responseBody ->
                                        log.error("Error Operator API response from server: {}", responseBody)
                                )
                                // throw original error
                                .then(response.createException())
                )
                .bodyToMono(WalletBalanceVo.class)
                .timeout(Duration.ofMillis(10000)) // TODO: timeout constant
                .block();

        if (responseVo != null) {
            log.info(responseVo.toString());
        }

        return responseVo;
    }
}
