package com.nextgen.gameaggregator.operator.wallet.bet;

import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class WalletBetAction {
    public WalletBalanceVo call(String callbackUrl, String signature, WalletBetDto dto) throws InsufficientBalanceException, InvalidOperatorResponseException {
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

        // throw exception if response is null
        Optional.ofNullable(responseVo).orElseThrow(InvalidOperatorResponseException::new);
        log.info(responseVo.toString());

        switch (responseVo.getStatus()) {
            case SC_OK -> {
                BigDecimal balance = responseVo.getData().getBalance();
                boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
                if (isNegativeBalance) throw new InsufficientBalanceException();
            }

            case SC_INSUFFICIENT_FUNDS -> throw new InsufficientBalanceException();

            default -> throw new InvalidOperatorResponseException(); // TODO: to add in specific exceptions
        }

        return responseVo;
    }
}
