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
import com.nextgen.gameaggregator.util.RequestLogVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
public class WalletBetResultAction {
    @Value("${testing.stub:false}")
    private Boolean useStub;

    @Value("${spring.profiles.active}")
    private String profilesActive;
    @Autowired
    private RequestService requestService;

    @Autowired
    private AgentApiCredentialService agentApiCredentialService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private CurrencyConversionService currencyConversionService;
    @Autowired
    private BetResultRetryLogService betResultRetryLogService;

    public WalletBalanceVo call(String traceId, Integer agentId, GameSession gameSession, BetInformation betInformation, ResultType resultType, HttpRequestLog httpRequestLog, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate)
            throws InvalidOperatorResponseException, InvalidAgentApiCredentialException, VendorCurrencyNotSupportException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return requestService.responseOperatorSub();
        }

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo;

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

        WalletBetResultDto dto = this.newWalletBetResultDto(traceId, gameSession, betInformation, resultType);
        currencyConversionService.doCurrencyConversionRateFromVendorForBetResult(dto, fromVendorConversionRate);
        //log.info("Request [" + apiUrl + EndPoints.WALLET_BET_RESULT + "]: " + dto);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);
        headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            httpRequestLog.setOperatorStart(startTime);

            String jsonApiResponse = new Gson().toJson(dto);
            httpRequestLog.setOperatorData(jsonApiResponse);
            httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.WALLET_BET_RESULT);

        }

        RequestLogVo requestLogVo = null;

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

            long endTime = System.currentTimeMillis();
            if (httpRequestLog != null) {
                if (apiResponse != null) {
                    httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());

                }
                httpRequestLog.setOperatorEnd(endTime);
            }

            requestLogVo = requestService.createRequestLogVo(
                    EndPoints.WALLET_BET_RESULT, apiUrl, dto, apiResponse, headerMap, startTime, endTime,
                    this.getClass().getPackage().getName(), profilesActive);

            //log.info("Response [" + apiUrl + EndPoints.WALLET_BET_RESULT + "]: " + apiResponse);

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson(apiResponse.getBody(), WalletBalanceVo.class);
            if (httpRequestLog != null) {
                httpRequestLog.setOperatorResponse(apiResponse.getBody());
                httpRequestLog.setOperatorResponseStatus(responseVo.getStatus());
                Optional.ofNullable(responseVo.getData()).ifPresent(data -> httpRequestLog.setOperatorTimestamp(data.getTimestamp()));

            }

            boolean specialCaseForPP = false;

            //if its PP and settledBet (endRound) and responseVo is null (not success responses from operator)
            //then will still consider success and assign default success params with 0 balance return out
            if (gameSession.getVendorId() == 1 && dto.getIsEndRound() == 1) {
                if (responseVo == null || !responseVo.getStatus().equals(ResponseCodes.Status.SC_OK)) {

                    responseVo = this.processForceSuccess(gameSession, traceId, betInformation);
                    specialCaseForPP = true;

                }
            }

            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            RequestService.validateResponse(responseVo);

            //3. validate username and currency
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            // only special handling for PP, the rest will still process as normal
            if (!specialCaseForPP) {
                requestService.operatorStatusException(responseVo.getStatus());
            }

            // 5. add conversion rate when returning the balance to vendor
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

            //RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException |
                 InvalidResponseException |
                 ResponseNotMatchRequestException invalidResponseException) {

            if (gameSession.getVendorId() == 1 && dto.getIsEndRound() == 1) {
                responseVo = this.processForceSuccess(gameSession, traceId, betInformation);
            } else {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
            }


        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {

            if (gameSession.getVendorId() == 1 && dto.getIsEndRound() == 1) {
                responseVo = this.processForceSuccess(gameSession, traceId, betInformation);

            } else if (resultType.equals(ResultType.WIN) || resultType.equals(ResultType.LOSE) || resultType.equals(ResultType.END)
                    && invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                responseVo = this.processForceSuccess(gameSession, traceId, betInformation);
                betResultRetryLogService.create(httpRequestLog, gameSession.getVendorId(), agentId, betInformation, EndPoints.WALLET_BET_RESULT);

            } else {
                throw new InvalidOperatorResponseException(invalidOperatorResponseException.getOperatorStatus());
            }

        } catch (Exception exception) {

            if (gameSession.getVendorId() == 1 && dto.getIsEndRound() == 1) {
                responseVo = this.processForceSuccess(gameSession, traceId, betInformation);
            } else {
                long endTime = System.currentTimeMillis();
                Integer defaultOperatorErrorResponse = ResponseCodes.Status.SC_UNKNOWN_ERROR.code;

                requestLogVo = requestService.createRequestLogVo(
                        EndPoints.WALLET_BET_RESULT, apiUrl, dto, null, headerMap, startTime, endTime,
                        this.getClass().getPackage().getName(), profilesActive);

                if (exception.getMessage().contains("java.util.concurrent.TimeoutException")) {
                    defaultOperatorErrorResponse = ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code;
                }

                //RequestService.failResponseLog(requestLogVo, exception);
                throw new InvalidOperatorResponseException(defaultOperatorErrorResponse);
            }

        }
        return responseVo;
    }

    public WalletBalanceVo callProcessEndRound(String traceId, Integer agentId, GameSession gameSession, BetInformation betInformation, ResultType resultType, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate)
            throws InvalidAgentApiCredentialException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return requestService.responseOperatorSub();
        }

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo;

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

        WalletBetResultDto dto = this.newWalletBetResultDto(traceId, gameSession, betInformation, resultType);
        currencyConversionService.doCurrencyConversionRateFromVendorForBetResult(dto, fromVendorConversionRate);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);
        headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());

        long startTime = System.currentTimeMillis();
        String jsonApiResponse = new Gson().toJson(dto);

        HttpRequestLog httpRequestLog = new HttpRequestLog();
        httpRequestLog.setAgentId(agentId);
        httpRequestLog.setOperatorStart(startTime);
        httpRequestLog.setOperatorData(jsonApiResponse);
        httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.WALLET_BET_RESULT);

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

            long endTime = System.currentTimeMillis();
            if (apiResponse != null) {
                httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());

            }
            httpRequestLog.setOperatorEnd(endTime);

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson(apiResponse.getBody(), WalletBalanceVo.class);
            if (!responseVo.getStatus().equals(ResponseCodes.Status.SC_OK)) {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
            } else {
                httpRequestLog.setOperatorResponse(apiResponse.getBody());
                httpRequestLog.setOperatorResponseStatus(responseVo.getStatus());
                Optional.ofNullable(responseVo.getData()).ifPresent(data -> httpRequestLog.setOperatorTimestamp(data.getTimestamp()));
            }

            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            RequestService.validateResponse(responseVo);
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

        } catch (Exception exception) {
            responseVo = this.processForceSuccess(gameSession, traceId, betInformation);
            betResultRetryLogService.create(httpRequestLog, gameSession.getVendorId(), agentId, betInformation, EndPoints.WALLET_BET_RESULT);

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
        walletBetResultDto.setToken(gameSession.getToken());
        walletBetResultDto.setGameCode(gameSession.getGameCode());
        walletBetResultDto.setBetTime(betInformation.getVendorBetTime());
        walletBetResultDto.setSettledTime(betInformation.getVendorSettleTime());

        return walletBetResultDto;
    }

    private BigDecimal stripZeroToString(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
