package com.nextgen.gameaggregator.operator.wallet.win;

import com.nextgen.gameaggregator.operator.constant.Endpoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@Slf4j
public class WalletWinAction {

    public WalletWinVo call(String callbackUrl, String signature, WalletWinDto dto) {
        log.info(dto.toString());

        WalletWinVo responseVo = WebClient.create(callbackUrl)
                .post()
                .uri(Endpoints.WALLET_WIN)
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
                .bodyToMono(WalletWinVo.class)
                .timeout(Duration.ofMillis(10000)) // TODO: timeout constant
                .block();

        if (responseVo != null) {
            log.info(responseVo.toString());
        }

        return responseVo;
    }
}
