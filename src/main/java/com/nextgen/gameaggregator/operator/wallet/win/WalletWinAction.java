package com.nextgen.gameaggregator.operator.wallet.win;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.AuthenticationService;
import com.nextgen.gameaggregator.service.OperatorRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class WalletWinAction {
    @Value("${testing.stub:false}")
    private Boolean useStub;

    @Value("${spring.profiles.active}")
    private String profilesActive;
    @Autowired
    OperatorRequestService operatorRequestService;

    @Autowired
    AuthenticationService authenticationService;

    public WalletBalanceVo call(AgentApiCredential agentApiCredential, WalletWinDto dto) throws InvalidOperatorResponseException {
//        log.info(dto.toString());
        // Call stub function instead if config file set to use stub
        if (useStub) {
            return operatorRequestService.responseOperatorSub();
        }
        WalletBalanceVo responseVo = null;

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());

        String responseString = WebClient.create(agentApiCredential.getCallbackUrl())
                .post()
                .uri(EndPoints.WALLET_WIN)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(EndPoints.HEADER_SIGNATURE, signature)
                .header(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey())
                .body(BodyInserters.fromValue(dto))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> Mono.empty())
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT)) // TODO: timeout constant
                .block();
        try {
            responseVo = new Gson().fromJson(responseString, WalletBalanceVo.class);

            //temp checking
            log.info("responseVo win = " + responseVo);
            // throw exception if response is null
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));

            operatorRequestService.validateResponse(responseVo);

            if ((!responseVo.getStatus().equals(ResponseCodes.Status.SC_OK)) ||
                    (!responseVo.getData().getUsername().equals(dto.getUsername())) ||
                    (!responseVo.getData().getCurrency().equals(dto.getCurrency()))) {
                throw new InvalidOperatorResponseException(responseVo.toString(), responseVo.getStatus().code);
            } else {
                operatorRequestService.operatorResponseLogging(true, EndPoints.WALLET_WIN, agentApiCredential.getCallbackUrl(), dto, responseString, profilesActive);
            }

        } catch (JsonSyntaxException | InvalidOperatorResponseException exception) {
            operatorRequestService.operatorResponseLogging(false, EndPoints.WALLET_WIN, agentApiCredential.getCallbackUrl(), dto, responseString, profilesActive);
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
        }

        return responseVo;

    }
}
