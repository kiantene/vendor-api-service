package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceData;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceDto;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetAction;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetDto;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.validation.Validation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class WalletService {
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private WalletBalanceAction walletBalanceAction;

    public BigDecimal getBalance(GameSession gameSession) throws InvalidOperatorResponseException {
        String traceId = UUID.randomUUID().toString();
        Integer agentId = gameSession.getAgentId();
        String callbackUrl = agentApiCredentialService.getCallbackUrl(agentId);
        String signature = "";

        WalletBalanceDto walletBalanceDto = new WalletBalanceDto();
        walletBalanceDto.setTraceId(traceId);
        walletBalanceDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBalanceDto.setCurrency(gameSession.getCurrencyCode());
        walletBalanceDto.setToken(gameSession.getToken());

        OperatorResponseVo<WalletBalanceData> responseVo = walletBalanceAction.call(callbackUrl, signature, walletBalanceDto);

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
