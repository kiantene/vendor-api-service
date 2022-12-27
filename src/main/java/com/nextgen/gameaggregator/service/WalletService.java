package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceData;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceDto;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.repository.AgentApiCredentialRepository;
import com.nextgen.gameaggregator.vo.OperatorResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import javax.validation.Validation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class WalletService {
    @Autowired
    AgentApiCredentialRepository agentApiCredentialRepository;
    @Autowired
    private WalletBalanceAction walletBalanceAction;

    public BigDecimal getBalance(GameSession gameSession) throws InvalidOperatorResponseException {
        String traceId = UUID.randomUUID().toString();

        Integer agentId = gameSession.getAgentId();
        final Integer STATUS_ACTIVE = 1; // TODO: to refactor
        AgentApiCredential credential = agentApiCredentialRepository.findByAgentIdAndStatus(agentId, STATUS_ACTIVE);
        String callbackUrl = credential.getCallbackUrl();

        //TODO WAYS TO GET VENDOR CODE
        WalletBalanceDto walletBalanceDto = new WalletBalanceDto(
                gameSession.getAgentPlayerUsername(),
                traceId,
                Long.parseLong(gameSession.getAgentId().toString()),
                "PP",
                gameSession.getCurrencyCode()
        );

        OperatorResponseVo<WalletBalanceData> responseVo = walletBalanceAction.call(callbackUrl, walletBalanceDto);

//        String responses;
//        responses = this.callRestApiService(walletBalanceDto, webClient);
//        this.validateResponse(responses);

//        if (this.walletBalanceVo.isStatus()) {
//            return walletBalanceVo.getResponse().getData().getBalance();
//        } else {
//            throw new InvalidOperatorResponseException();
//        }
        return responseVo.getData().getBalance();
    }

//    private void validateResponse(String response)
//    {
//        //TODO VALIDATE OPERATOR'S RESPONSES
//        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
//        this.walletBalanceVo = new Gson().fromJson(response, WalletBalanceVo.class);
//    }
}
