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
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_INSUFFICIENT_FUNDS;

@Service
@Slf4j
public class WalletBetAction {

    @Value("${testing.stub}")
    private Boolean useStub;

    public WalletBalanceVo call(String callbackUrl, String signature, WalletBetDto dto) throws InsufficientBalanceException, InvalidOperatorResponseException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return this.stub();
        }
//        log.info(dto.toString());
        WalletBalanceVo responseVo = null;
        try {

            responseVo = WebClient.create(callbackUrl)
                    .post()
                    .uri(Endpoints.WALLET_BET)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(Endpoints.HEADER_SIGNATURE, signature)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .onStatus(HttpStatus::isError,
                            response -> {
                                HttpStatus clientResponsestatus = response.statusCode();
                                return response.bodyToMono(String.class).map(body ->
                                        new InvalidOperatorResponseException
                                                ("response status :" + clientResponsestatus + ", response body :" + body, ResponseCodes.Status.SC_INVALID_RESPONSE.code));
                            })
                    .bodyToMono(WalletBalanceVo.class)
                    .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                    .block();

        } catch (Exception exception) {
            //TODO (by Alex), proper throw InvalidOperatorResponseException
            throw new InvalidOperatorResponseException(exception.getMessage(), ResponseCodes.Status.SC_INVALID_RESPONSE.code);
        }
        // throw exception if response is null
        Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
//        log.info(responseVo.toString());

        switch (responseVo.getStatus()) {
            case SC_OK -> {
                BigDecimal balance = responseVo.getData().getBalance();
                //TODO to be discuss whether should system pre handle negative if
                boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
                if (isNegativeBalance) throw new InsufficientBalanceException(responseVo.toString());
            }
            case SC_INSUFFICIENT_FUNDS -> throw new InsufficientBalanceException(responseVo.toString());
            default -> throw new InvalidOperatorResponseException(responseVo.toString(), responseVo.getStatus().code);
        }

        return responseVo;
    }

    public WalletBalanceVo stub() throws InsufficientBalanceException, InvalidOperatorResponseException {
        WalletBalanceVo.ResponseData responseData = new WalletBalanceVo.ResponseData();
        responseData.setBalance(BigDecimal.ONE);
        WalletBalanceVo balanceVo = new WalletBalanceVo();
        balanceVo.setData(responseData);

        if (responseData.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException();
        } else if (responseData.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidOperatorResponseException();
        }

        return balanceVo;
    }
}
