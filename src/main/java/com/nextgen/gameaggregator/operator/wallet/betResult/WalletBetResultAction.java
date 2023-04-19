package com.nextgen.gameaggregator.operator.wallet.betResult;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.RawSettledBet;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorLogVo;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.AgentApiCredentialService;
import com.nextgen.gameaggregator.service.AuthenticationService;
import com.nextgen.gameaggregator.service.OperatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class WalletBetResultAction {
    @Value("${testing.stub:false}")
    private Boolean useStub;

    @Value("${spring.profiles.active}")
    private String profilesActive;
    @Autowired
    OperatorService operatorService;

    @Autowired
    AgentApiCredentialService agentApiCredentialService;

    @Autowired
    AuthenticationService authenticationService;

    public WalletBalanceVo call(String traceId, Integer agentId, GameSession gameSession, RawSettledBet rawSettledBet)
            throws InvalidOperatorResponseException, InvalidAgentApiCredentialException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return operatorService.responseOperatorSub();
        }

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        WalletBetResultDto dto = this.newWalletBetResultDtoForFullBetDto(traceId, gameSession, rawSettledBet);
        WalletBalanceVo responseVo = null;

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        log.info("walletBetResultDto : " + dto);

        ResponseEntity apiResponse = WebClient.create(agentApiCredential.getCallbackUrl())
                .post()
                .uri(Endpoints.WALLET_BET_RESULT)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(Endpoints.HEADER_SIGNATURE, signature)
                .body(BodyInserters.fromValue(dto))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        OperatorLogVo operatorLogVo = operatorService.createOperatorLogVo(
                Endpoints.WALLET_BET, agentApiCredential.getCallbackUrl(), dto, apiResponse, signature, profilesActive);


        try {
            // 1. validate HTTP Response Code
            operatorService.validateOperatorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), WalletBalanceVo.class);
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            operatorService.validateResponse(responseVo);

            System.out.println("apiResponse = " + apiResponse);
            System.out.println("dto = " + dto);

            //3. validate username and currency
            operatorService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            operatorService.operatorStatusException(responseVo.getStatus());


        } catch (HttpResponseStatusCodeException httpResponseStatusCodeException) {
            operatorService.failResponseLog(operatorLogVo, httpResponseStatusCodeException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (JsonSyntaxException jsonSyntaxException) {
            operatorService.failResponseLog(operatorLogVo, jsonSyntaxException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidTokenException invalidTokenException) {
            operatorService.failResponseLog(operatorLogVo, invalidTokenException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_TOKEN.code);

        } catch (InvalidSignatureException invalidSignatureException) {
            operatorService.failResponseLog(operatorLogVo, invalidSignatureException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_SIGNATURE.code);

        } catch (InvalidPlayerException invalidPlayerException) {
            operatorService.failResponseLog(operatorLogVo, invalidPlayerException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_USER_NOT_EXISTS.code);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            operatorService.failResponseLog(operatorLogVo, disabledAgentPlayerException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (GameNotSupportedException gameNotSupportedException) {
            operatorService.failResponseLog(operatorLogVo, gameNotSupportedException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            operatorService.failResponseLog(operatorLogVo, insufficientBalanceException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidRequestException invalidRequestException) {
            operatorService.failResponseLog(operatorLogVo, invalidRequestException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_REQUEST.code);

        } catch (BetNotFoundException betNotFoundException) {
            operatorService.failResponseLog(operatorLogVo, betNotFoundException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_TRANSACTION_NOT_EXISTS.code);

        } catch (SystemMaintenanceException systemMaintenanceException) {
            operatorService.failResponseLog(operatorLogVo, systemMaintenanceException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNDER_MAINTENANCE.code);

        } catch (DuplicateTransactionException duplicateTransactionException) {
            operatorService.failResponseLog(operatorLogVo, duplicateTransactionException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_TRANSACTION_DUPLICATED.code);

        } catch (DuplicateRequestException duplicateRequestException) {
            operatorService.failResponseLog(operatorLogVo, duplicateRequestException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_DUPLICATE_REQUEST.code);

        } catch (InvalidCurrencyException invalidCurrencyException) {
            operatorService.failResponseLog(operatorLogVo, invalidCurrencyException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_WRONG_CURRENCY.code);

        } catch (ResponseNotMatchRequestException responseNotMatchRequestException) {
            operatorService.failResponseLog(operatorLogVo, responseNotMatchRequestException.getClass().getName() +
                    " [" + responseNotMatchRequestException.getMessage() + "]");
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        }
//        catch (Exception exception){
//            operatorService.failResponseLog(operatorLogVo, exception.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
//        }

        return responseVo;

    }

    private WalletBetResultDto newWalletBetResultDtoForFullBetDto(String traceId, GameSession gameSession, RawSettledBet rawSettledBet) {

        BigDecimal betAmount = (ObjectUtils.isEmpty(rawSettledBet.getWinLoss())) ? null : new BigDecimal(rawSettledBet.getBetAmount().stripTrailingZeros().toPlainString());
        BigDecimal effectiveTurnover = (ObjectUtils.isEmpty(rawSettledBet.getEffectiveTurnover())) ? null : new BigDecimal(rawSettledBet.getEffectiveTurnover().stripTrailingZeros().toPlainString());
        BigDecimal winAmount = (ObjectUtils.isEmpty(rawSettledBet.getWinAmount())) ? null : new BigDecimal(rawSettledBet.getWinAmount().stripTrailingZeros().toPlainString());
        BigDecimal winLossAmount = (ObjectUtils.isEmpty(rawSettledBet.getWinLoss())) ? null : new BigDecimal(rawSettledBet.getWinLoss().stripTrailingZeros().toPlainString());
        BigDecimal jackpotAmount = (ObjectUtils.isEmpty(rawSettledBet.getJackpotAmount())) ? null : new BigDecimal(rawSettledBet.getJackpotAmount().stripTrailingZeros().toPlainString());

        WalletBetResultDto walletBetResultDto = new WalletBetResultDto();
        walletBetResultDto.setTraceId(traceId);
        walletBetResultDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBetResultDto.setTransactionId(rawSettledBet.getInternalTransactionId());
        walletBetResultDto.setExternalTransactionId(rawSettledBet.getVendorBetId());
        walletBetResultDto.setExternalRoundId(rawSettledBet.getRoundId());
        walletBetResultDto.setBetAmount(betAmount);
        walletBetResultDto.setWinAmount(winAmount);
        walletBetResultDto.setEffectiveTurnover(effectiveTurnover);
        walletBetResultDto.setJackpotAmount(jackpotAmount);
        walletBetResultDto.setWinLoss(winLossAmount);
        walletBetResultDto.setResultType(WinType.RESULT_TYPE_VALUE.get(rawSettledBet.getResultType()));
        walletBetResultDto.setIsFreespin(rawSettledBet.getIsFreespin());
        walletBetResultDto.setIsEndRound(BetStatus.UNSETTLED.isValueOf(rawSettledBet.getStatus()) ? 0 : 1);
        walletBetResultDto.setCurrency(gameSession.getCurrencyCode());
        walletBetResultDto.setToken(gameSession.getToken());
        walletBetResultDto.setGameCode(gameSession.getGameCode());
        walletBetResultDto.setBetTime(rawSettledBet.getVendorBetTime());
        walletBetResultDto.setSettledTime(rawSettledBet.getVendorSettleTime());

        return walletBetResultDto;
    }
}
