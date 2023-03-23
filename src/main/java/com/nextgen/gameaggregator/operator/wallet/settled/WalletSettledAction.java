package com.nextgen.gameaggregator.operator.wallet.settled;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
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
public class WalletSettledAction {
    @Value("${testing.stub:false}")
    private Boolean useStub;

    public WalletBalanceVo call(String callbackUrl, String signature, WalletSettledDto dto) throws InvalidOperatorResponseException {
//        log.info(dto.toString());
        // Call stub function instead if config file set to use stub
        if (useStub) {
//            return this.stub();
        }
        WalletBalanceVo responseVo = null;

            String responseString = WebClient.create(callbackUrl)
                    .post()
                    .uri(Endpoints.WALLET_WIN)
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
                    .timeout(Duration.ofMillis(Endpoints.TIMEOUT)) // TODO: timeout constant
                    .block();

        try {
            responseVo = new Gson().fromJson(responseString, WalletBalanceVo.class);
            // throw exception if response is null
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));

        } catch (JsonSyntaxException jsonSyntaxException) {
            log.error("Operator URL :" + callbackUrl);
            log.error("Invalid Operator Response :" + responseString);
            new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
        }

        if(!responseVo.getStatus().equals(ResponseCodes.Status.SC_OK)){
            log.error("Operator URL " + callbackUrl);
            log.error("Invalid Operator Response :" + responseString);
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
