package com.nextgen.gameaggregator.operator.wallet.bet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${testing.stub:false}")
    private Boolean useStub;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    public WalletBalanceVo call(String callbackUrl, String signature, WalletBetDto dto) throws InsufficientBalanceException, InvalidOperatorResponseException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return ValidationUtils.responseOperatorSub();
        }
//        log.info(dto.toString());
        WalletBalanceVo responseVo = null;

        String responseString = WebClient.create(callbackUrl)
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
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        try {
            responseVo = new Gson().fromJson(responseString, WalletBalanceVo.class);
            // throw exception if response is null
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));

            ValidationUtils.validateResponse(responseVo);

            switch (responseVo.getStatus()) {
                case SC_OK -> {
                    //To validate the username and currency is match with request
                    if ((!responseVo.getData().getUsername().equals(dto.getUsername())) ||
                            (!responseVo.getData().getCurrency().equals(dto.getCurrency()))) {
                        throw new InvalidOperatorResponseException(responseVo.toString(), responseVo.getStatus().code);
                    } else {
                        ValidationUtils.operatorResponseLogging(true, Endpoints.WALLET_BET, callbackUrl, dto, responseString, profilesActive);
                    }
                    BigDecimal balance = responseVo.getData().getBalance();
                    //TODO to be discuss whether should system pre handle negative if
                    boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
                    if (isNegativeBalance) {
                        ValidationUtils.operatorResponseLogging(false, Endpoints.WALLET_BET, callbackUrl, dto, responseString, profilesActive);
                        throw new InsufficientBalanceException(responseVo.toString());
                    }
                }
                case SC_INSUFFICIENT_FUNDS -> {
                    ValidationUtils.operatorResponseLogging(false, Endpoints.WALLET_BET, callbackUrl, dto, responseString, profilesActive);
                    throw new InsufficientBalanceException(responseVo.toString());
                }
                default -> {
                    throw new InvalidOperatorResponseException(responseVo.toString(), responseVo.getStatus().code);
                }
            }

        } catch (JsonSyntaxException | InvalidOperatorResponseException exception) {
            ValidationUtils.operatorResponseLogging(false, Endpoints.WALLET_BET, callbackUrl, dto, responseString, profilesActive);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
        }


        return responseVo;
    }

}
