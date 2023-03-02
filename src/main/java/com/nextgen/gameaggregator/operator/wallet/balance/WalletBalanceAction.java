package com.nextgen.gameaggregator.operator.wallet.balance;

import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
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
public class WalletBalanceAction {

    @Value("${testing.stub:false}")
    private Boolean useStub;

    public WalletBalanceVo call(String callbackUrl, String signature, WalletBalanceDto dto) throws InvalidOperatorResponseException {
//        log.info(dto.toString());
        // Call stub function instead if config file set to use stub
        if (useStub) {
            return this.stub();
        }

        WalletBalanceVo responseVo = null;
        try {
         responseVo = WebClient.create(callbackUrl)
                .post()
                .uri(Endpoints.WALLET_BALANCE)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(Endpoints.HEADER_SIGNATURE, signature)
                .body(BodyInserters.fromValue(dto))
                .retrieve()
                .onStatus(HttpStatus::isError,
                        response -> {
                            HttpStatus clientResponseStatus = response.statusCode();
                            return response.bodyToMono(String.class).map(body ->
                                    new InvalidOperatorResponseException
                                            ("response status :" + clientResponseStatus + ", response body :" + body, ResponseCodes.Status.SC_INVALID_RESPONSE.code));
                        })
                .bodyToMono(WalletBalanceVo.class)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        } catch (Exception exception) {
            //TODO (by Alex), proper throw InvalidOperatorResponseException
            throw new InvalidOperatorResponseException(exception.getMessage(), ResponseCodes.Status.SC_INVALID_RESPONSE.code);
        }

        Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
//        log.info(responseVo.toString());

        if(!responseVo.getStatus().equals(ResponseCodes.Status.SC_OK)){
            throw new InvalidOperatorResponseException(responseVo.toString(), responseVo.getStatus().code);
        }
        return responseVo;
    }

    public WalletBalanceVo stub() throws InvalidOperatorResponseException {
        WalletBalanceVo.ResponseData responseData = new WalletBalanceVo.ResponseData();
        responseData.setBalance(BigDecimal.ONE);
        WalletBalanceVo balanceVo = new WalletBalanceVo();
        balanceVo.setData(responseData);

        return balanceVo;
    }
}
