package com.nextgen.gameaggregator.sport.operator.wallet.settle;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.Agent;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.AgentPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultDto;
import com.nextgen.gameaggregator.service.AgentApiCredentialService;
import com.nextgen.gameaggregator.service.AgentPlayerService;
import com.nextgen.gameaggregator.service.AuthenticationService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetMariaDB;
import com.nextgen.gameaggregator.util.RequestLogVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
public class SportWalletSettleAction {

    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private RequestService requestService;

    public WalletBalanceVo call(String traceId, SportUnsettledBetMariaDB unsettledBet, SportSettledBet settledBet) throws InvalidAgentApiCredentialException, RecordNotFoundException {
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo;

        AgentPlayer agentPlayer = agentPlayerService.get(unsettledBet.getAgentPlayerId());
        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(unsettledBet.getAgentId());
        String apiUrl = agentApiCredential.getCallbackUrl();

        WalletBetResultDto dto = this.newSportWalletSettleDto(traceId, unsettledBet, settledBet, agentApiCredential.getAgent(), agentPlayer);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);

        long startTime = System.currentTimeMillis();

        RequestLogVo requestLogVo = null;

        try {
            ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.WALLET_BET_RESULT)
                    .header(EndPoints.HEADER_SIGNATURE, signature)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                    .toEntity(String.class)
                    .retry(3)
                    .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                    .block();

            long endTime = System.currentTimeMillis();

            requestService.validateVendorHttpStatusResponse(apiResponse);

            responseVo = new Gson().fromJson(apiResponse.getBody(), WalletBalanceVo.class);

            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            RequestService.validateResponse(responseVo);

            //3. validate username and currency
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

            // 5. add conversion rate when returning the balance to vendor
//            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

        } catch (HttpResponseStatusCodeException | InvalidOperatorResponseException | InvalidResponseException |
                 ResponseNotMatchRequestException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private SportWalletSettleDto newSportWalletSettleDto(String traceId, SportUnsettledBetMariaDB unsettledBet, SportSettledBet settledBet, Agent agent, AgentPlayer agentPlayer) {

        // add conversion rate when sending all the figures to operator
        BigDecimal betAmount = (ObjectUtils.isEmpty(unsettledBet.getActualBetAmount())) ? null : this.stripZeroToString(unsettledBet.getBetAmount());
        BigDecimal effectiveTurnover = (ObjectUtils.isEmpty(unsettledBet.getEffectiveTurnover())) ? null : this.stripZeroToString(unsettledBet.getEffectiveTurnover());
        BigDecimal winAmount = (ObjectUtils.isEmpty(settledBet.getWinAmount())) ? null : this.stripZeroToString(settledBet.getWinAmount());
        assert winAmount != null;
        assert betAmount != null;
        BigDecimal winLossAmount = winAmount.compareTo(BigDecimal.ZERO) > 0 ? winAmount : betAmount.negate();
        ResultType resultType = winAmount.compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.LOSE;

        SportWalletSettleDto sportWalletSettleDto = new SportWalletSettleDto();
        sportWalletSettleDto.setTraceId(traceId);
        sportWalletSettleDto.setUsername(agentPlayer.getUsername());
        sportWalletSettleDto.setBetId(unsettledBet.getId());
        sportWalletSettleDto.setTransactionId(traceId);
        sportWalletSettleDto.setExternalTransactionId(settledBet.getExternalTransactionId());
        sportWalletSettleDto.setRoundId(unsettledBet.getRoundId());
        sportWalletSettleDto.setBetAmount(betAmount);
        sportWalletSettleDto.setWinAmount(winAmount);
        sportWalletSettleDto.setEffectiveTurnover(effectiveTurnover);
        sportWalletSettleDto.setJackpotAmount(BigDecimal.ZERO);
        sportWalletSettleDto.setWinLoss(winLossAmount);
        sportWalletSettleDto.setResultType(resultType);
        sportWalletSettleDto.setIsFreespin(0);
        sportWalletSettleDto.setIsEndRound(1);
        sportWalletSettleDto.setCurrency(agent.getCurrency().getCode());
        sportWalletSettleDto.setToken(null);
        sportWalletSettleDto.setGameCode(null);
        sportWalletSettleDto.setBetTime(settledBet.getVendorBetTime());
        sportWalletSettleDto.setSettledTime(settledBet.getVendorSettleTime());

        return sportWalletSettleDto;
    }

    private BigDecimal stripZeroToString(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
