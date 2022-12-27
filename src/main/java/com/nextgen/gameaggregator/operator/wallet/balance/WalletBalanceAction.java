package com.nextgen.gameaggregator.operator.wallet.balance;

import com.nextgen.gameaggregator.vo.OperatorResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@Slf4j
public class WalletBalanceAction {
    private static final String ENDPOINT = "/wallet/balance/";
    public OperatorResponseVo<WalletBalanceData> call(String callbackUrl, WalletBalanceDto dto) {
        OperatorResponseVo<WalletBalanceData> responseVo = new OperatorResponseVo<>();

        WebClient webClient = WebClient.create(callbackUrl);
        String data = webClient.post()
                .uri(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
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
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(10000)) // TODO: timeout constant
                .block();

        log.info(data);
        WalletBalanceData walletBetData = new WalletBalanceData();
        walletBetData.setBalance(new BigDecimal("1000"));

        responseVo.setData(walletBetData);

        return responseVo;
    }
}
