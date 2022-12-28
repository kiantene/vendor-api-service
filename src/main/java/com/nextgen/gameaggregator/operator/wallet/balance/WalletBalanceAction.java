package com.nextgen.gameaggregator.operator.wallet.balance;

import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;

@Service
@Slf4j
public class WalletBalanceAction {
    public OperatorResponseVo<WalletBalanceData> call(String callbackUrl, String signature, WalletBalanceDto dto) {
        OperatorResponseVo<WalletBalanceData> responseVo = new OperatorResponseVo<>();

        OperatorResponseVo data = WebClient.create(callbackUrl)
                .post()
                .uri(Endpoints.WALLET_BALANCE)
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
                .bodyToMono(OperatorResponseVo.class)
                .timeout(Duration.ofMillis(10000)) // TODO: timeout constant
                .block();

        log.info(data.toString());
        responseVo.setStatus(data.getStatus());
        responseVo.setTraceId(data.getTraceId());
        responseVo.setMessage(data.getMessage());

        LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) data.getData();

        WalletBalanceData walletBalanceData = new WalletBalanceData();
        walletBalanceData.setUsername(dataMap.get("username").toString());
        walletBalanceData.setCurrency(dataMap.get("currency").toString());
        walletBalanceData.setBalance(new BigDecimal(dataMap.get("balance").toString()));
        responseVo.setData(walletBalanceData);

        return responseVo;
    }
}
