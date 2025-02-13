package com.nextgen.gameaggregator.operator.wallet.betResult;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class WalletBetResultAction {
    private final RequestService requestService;
    private final AgentApiCredentialService agentApiCredentialService;
    private final AuthenticationService authenticationService;
    private final CurrencyConversionService currencyConversionService;
    private final BetResultRetryLogService betResultRetryLogService;
    private final Set<Integer> forceSuccessResultTypeList;
    private final Set<Integer> betWinVendorList;
    private final Set<Integer> betLoseVendorList;

    @Autowired
    public WalletBetResultAction(RequestService requestService,
                                 AgentApiCredentialService agentApiCredentialService,
                                 AuthenticationService authenticationService,
                                 CurrencyConversionService currencyConversionService,
                                 BetResultRetryLogService betResultRetryLogService) {

        this.requestService = requestService;
        this.agentApiCredentialService = agentApiCredentialService;
        this.authenticationService = authenticationService;
        this.currencyConversionService = currencyConversionService;
        this.betResultRetryLogService = betResultRetryLogService;
        this.forceSuccessResultTypeList = new HashSet<>();
        this.betWinVendorList = new HashSet<>();
        this.betLoseVendorList = new HashSet<>();

        this.forceSuccessResultTypeList.add(ResultType.WIN.code);
        this.forceSuccessResultTypeList.add(ResultType.LOSE.code);
        this.forceSuccessResultTypeList.add(ResultType.END.code);

        this.betWinVendorList.addAll(Set.of(32, 55, 7, 19, 48, 61, 47, 69, 74));
        this.betLoseVendorList.addAll(Set.of(7, 19, 48, 61, 47, 69, 74));
    }

    public WalletBalanceVo generateOperatorBetResultInfoAndForceRetry(String traceId, Integer agentId, GameSession gameSession, BetInformation betInformation, ResultType resultType, HttpRequestLog httpRequestLog, BigDecimal fromVendorConversionRate) {
        WalletBalanceVo responseVo = new WalletBalanceVo();
        WalletBetResultDto dto = this.newWalletBetResultDto(traceId, gameSession, betInformation, resultType);
        String jsonApiResponse = null;
        currencyConversionService.doCurrencyConversionRateFromVendorForBetResult(dto, fromVendorConversionRate);

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            jsonApiResponse = new Gson().toJson(dto);

        }

        responseVo = this.processForceSuccess(gameSession, traceId, betInformation);
        betResultRetryLogService.create(jsonApiResponse, gameSession.getVendorId(), agentId, betInformation.getBetId(), betInformation.getRoundId(), betInformation.getInternalTransactionId(), EndPoints.WALLET_BET_RESULT);

        return responseVo;
    }

    public WalletBalanceVo call(String traceId, Integer agentId, GameSession gameSession, BetInformation betInformation, ResultType resultType, HttpRequestLog httpRequestLog, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate)
            throws InvalidOperatorResponseException, InvalidAgentApiCredentialException, VendorCurrencyNotSupportException {

        WalletBalanceVo responseVo = new WalletBalanceVo();
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

        WalletBetResultDto dto = this.newWalletBetResultDto(traceId, gameSession, betInformation, resultType);
        currencyConversionService.doCurrencyConversionRateFromVendorForBetResult(dto, fromVendorConversionRate);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        ResponseCodes.Status operatorStatus = ResponseCodes.Status.SC_UNKNOWN_ERROR;

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            httpRequestLog.setOperatorStart(startTime);

            String jsonApiResponse = new Gson().toJson(dto);
            httpRequestLog.setOperatorData(jsonApiResponse);
            httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.WALLET_BET_RESULT);

        }

        boolean isError = false;

        // if match useStub and username prefix will skip call to stub
        if (requestService.shouldSkipStubCall(dto.getUsername())) {
            return requestService.responseOperatorSub();
        }

        try {
            ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.WALLET_BET_RESULT)
                    .header(EndPoints.HEADER_SIGNATURE, signature)
                    .header(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                    .toEntity(String.class)
                    .retry(3)
                    .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                    .onErrorResume(TimeoutException.class, e -> {
                        isTimeout.set(true);
                        return Mono.error(e);
                    })
                    .block();

            long endTime = System.currentTimeMillis();
            if (httpRequestLog != null) {
                httpRequestLog.setOperatorEnd(endTime);
                if (apiResponse != null) {
                    httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());
                }
            }

            if (isTimeout.get()) {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code);
            }

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson(apiResponse.getBody(), WalletBalanceVo.class);
            if (httpRequestLog != null) {
                httpRequestLog.setOperatorResponse(apiResponse.getBody());
                httpRequestLog.setOperatorResponseStatus(responseVo.getStatus());
                Optional.ofNullable(responseVo.getData()).ifPresent(data -> httpRequestLog.setOperatorTimestamp(data.getTimestamp()));

            }

            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            RequestService.validateResponse(responseVo);

            //3. validate username and currency
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            //4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

            //5. add conversion rate when returning the balance to vendor
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

            if (dto.getBetAmount().compareTo(BigDecimal.ZERO) > 0) { //if bet amount exist in result
                //operator check negative value
                BigDecimal balance = responseVo.getData().getBalance();
                boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
                if (isNegativeBalance) {
                    throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
                }
            } //else do nothing

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException |
                 InvalidResponseException |
                 ResponseNotMatchRequestException invalidResponseException) {
            isError = true;
            operatorStatus = ResponseCodes.Status.SC_INVALID_RESPONSE;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            isError = true;
            operatorStatus = ResponseCodes.Status.checkCodeStatus(invalidOperatorResponseException.getOperatorStatus());

        } catch (Exception exception) {
            isError = true;
            operatorStatus = ResponseCodes.Status.SC_UNKNOWN_ERROR;

        } finally {
            if (isError) {
                boolean shouldForceSuccess = false;

                if (this.forceSuccessResultTypeList.contains(resultType.code)) {
                    // WIN, LOSE, END resultType will be force success.
                    shouldForceSuccess = true;

                } else if ((this.betWinVendorList.contains(gameSession.getVendorId()) && resultType.code.equals(ResultType.BET_WIN.code))) {
                    // BET_WIN resultType will be force success, but only apply to certain vendors
                    shouldForceSuccess = true;

                } else if ((this.betLoseVendorList.contains(gameSession.getVendorId()) && resultType.code.equals(ResultType.BET_LOSE.code))) {
                    // BET_WIN resultType will be force success, but only apply to certain vendors
                    shouldForceSuccess = true;

                } else {
                    // normal BET_WIN and BET_LOSE should not be force success
                    throw new InvalidOperatorResponseException(operatorStatus.code);
                }

                if (shouldForceSuccess) {
                    responseVo = this.processForceSuccess(gameSession, traceId, betInformation);

                    if (httpRequestLog != null) {
                        betResultRetryLogService.create(httpRequestLog.getOperatorData(), gameSession.getVendorId(), agentId, betInformation.getBetId(), betInformation.getRoundId(), betInformation.getInternalTransactionId(), EndPoints.WALLET_BET_RESULT);
                    }
                }

            } else {
                //this is not error.
            }
        }
        return responseVo;
    }

    public WalletBalanceVo callProcessEndRound(String traceId, Integer agentId, GameSession gameSession, BetInformation betInformation, ResultType resultType, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate, HttpRequestLog httpRequestLog)
            throws InvalidAgentApiCredentialException {

        WalletBalanceVo responseVo;
        Long startTime = System.currentTimeMillis();
        Long endTime = 0L;

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

        WalletBetResultDto dto = this.newWalletBetResultDto(traceId, gameSession, betInformation, resultType);
        currencyConversionService.doCurrencyConversionRateFromVendorForBetResult(dto, fromVendorConversionRate);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        String jsonApiResponse = new Gson().toJson(dto);

        httpRequestLog.setOperatorStart(startTime);
        httpRequestLog.setOperatorData(jsonApiResponse);
        httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.WALLET_BET_RESULT);

        // if match useStub and username prefix will skip call to stub
        if (requestService.shouldSkipStubCall(dto.getUsername())) {
            return requestService.responseOperatorSub();
        }

        try {
            ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.WALLET_BET_RESULT)
                    .header(EndPoints.HEADER_SIGNATURE, signature)
                    .header(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                    .toEntity(String.class)
                    .retry(3)
                    .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                    .block();

            endTime = System.currentTimeMillis();
            if (apiResponse != null) {
                httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());

            }

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson(apiResponse.getBody(), WalletBalanceVo.class);
            httpRequestLog.setOperatorResponse(apiResponse.getBody());
            httpRequestLog.setOperatorResponseStatus(responseVo.getStatus());

            if (!responseVo.getStatus().equals(ResponseCodes.Status.SC_OK)) {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
            } else {
                Optional.ofNullable(responseVo.getData()).ifPresent(data -> httpRequestLog.setOperatorTimestamp(data.getTimestamp()));
            }

            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            RequestService.validateResponse(responseVo);
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

        } catch (Exception exception) {
            responseVo = this.processForceSuccess(gameSession, traceId, betInformation);
            betResultRetryLogService.create(httpRequestLog.getOperatorData(), gameSession.getVendorId(), agentId, betInformation.getBetId(), betInformation.getRoundId(), betInformation.getInternalTransactionId(), EndPoints.WALLET_BET_RESULT);

        } finally {
            endTime = (endTime.equals(0L) ? System.currentTimeMillis() : endTime);
            httpRequestLog.setOperatorEnd(endTime);
        }

        return responseVo;
    }

    private WalletBalanceVo processForceSuccess(GameSession gameSession, String traceId, BetInformation betInformation) {

        WalletBalanceVo responseVo = new WalletBalanceVo();
        WalletBalanceVo.ResponseData data = new WalletBalanceVo.ResponseData();
        BigDecimal balance = (betInformation.getBalance() == null) ? BigDecimal.ZERO : betInformation.getBalance();

        data.setBalance(balance);
        data.setUsername(gameSession.getAgentPlayerUsername());
        data.setCurrency(gameSession.getCurrencyCode());
        data.setTimestamp(System.currentTimeMillis());

        responseVo.setTraceId(traceId);
        responseVo.setStatus(ResponseCodes.Status.SC_OK);
        responseVo.setMessage(ResponseCodes.Status.SC_OK.description);
        responseVo.setData(data);

        return responseVo;
    }


    private WalletBetResultDto newWalletBetResultDto(String traceId, GameSession gameSession, BetInformation betInformation, ResultType resultType) {

        // add conversion rate when sending all the figures to operator
        BigDecimal betAmount = (ObjectUtils.isEmpty(betInformation.getBetAmount())) ? null : this.stripZeroToString(betInformation.getBetAmount());
        BigDecimal effectiveTurnover = (ObjectUtils.isEmpty(betInformation.getEffectiveTurnover())) ? null : this.stripZeroToString(betInformation.getEffectiveTurnover());
        BigDecimal winAmount = (ObjectUtils.isEmpty(betInformation.getWinAmount())) ? null : this.stripZeroToString(betInformation.getWinAmount());
        BigDecimal winLossAmount = (ObjectUtils.isEmpty(betInformation.getWinLoss())) ? null : this.stripZeroToString(betInformation.getWinLoss());
        BigDecimal jackpotAmount = (ObjectUtils.isEmpty(betInformation.getJackpotAmount())) ? null : this.stripZeroToString(betInformation.getJackpotAmount());

        WalletBetResultDto walletBetResultDto = new WalletBetResultDto();
        walletBetResultDto.setTraceId(traceId);
        walletBetResultDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBetResultDto.setBetId(betInformation.getBetId());
        walletBetResultDto.setTransactionId(betInformation.getInternalTransactionId());
        walletBetResultDto.setExternalTransactionId(betInformation.getExternalTransactionId());
        walletBetResultDto.setRoundId(betInformation.getRoundId());
        walletBetResultDto.setBetAmount(betAmount);
        walletBetResultDto.setWinAmount(winAmount);
        walletBetResultDto.setEffectiveTurnover(effectiveTurnover);
        walletBetResultDto.setJackpotAmount(jackpotAmount);
        walletBetResultDto.setWinLoss(winLossAmount);
        walletBetResultDto.setResultType(resultType);
        walletBetResultDto.setIsFreespin(betInformation.getIsFreespin());
        walletBetResultDto.setIsEndRound(BetStatus.UNSETTLED.isValueOf(betInformation.getStatus()) ? 0 : 1);
        walletBetResultDto.setCurrency(gameSession.getCurrencyCode());
        walletBetResultDto.setToken((betInformation.getGameSessionToken() != null) ? betInformation.getGameSessionToken() : gameSession.getToken());
        walletBetResultDto.setGameCode(gameSession.getGameCode());
        walletBetResultDto.setBetTime(betInformation.getVendorBetTime());
        walletBetResultDto.setSettledTime(betInformation.getVendorSettleTime());

        return walletBetResultDto;
    }

    private BigDecimal stripZeroToString(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
