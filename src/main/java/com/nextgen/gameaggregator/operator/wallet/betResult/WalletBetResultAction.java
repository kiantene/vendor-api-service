package com.nextgen.gameaggregator.operator.wallet.betResult;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
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

    public WalletBalanceVo call(String traceId, Integer agentId, GameSession gameSession, BetInformation betInformation, ResultType resultType, HttpRequestLog httpRequestLog, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate)
            throws InvalidOperatorResponseException, InvalidAgentApiCredentialException, VendorCurrencyNotSupportException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return requestService.responseOperatorSub();
        }

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo;

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredential.getCallbackUrl();

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

            }

            boolean specialCaseForPP = false;

            //if its PP and settledBet (endRound) and responseVo is null (not success responses from operator)
            //then will still consider success and assign default success params with 0 balance return out
            if (gameSession.getVendorId() == 1 && dto.getIsEndRound() == 1) {
                if (responseVo == null || !responseVo.getStatus().equals(ResponseCodes.Status.SC_OK)) {

                    responseVo = this.ppEndRoundForceSuccess(gameSession, responseVo, apiResponse, traceId);
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
                responseVo = this.ppEndRoundForceSuccess(gameSession, null, null, traceId);
            } else {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
            }


        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {

            if (gameSession.getVendorId() == 1 && dto.getIsEndRound() == 1) {
                responseVo = this.ppEndRoundForceSuccess(gameSession, null, null, traceId);
            } else {
                throw new InvalidOperatorResponseException(invalidOperatorResponseException.getOperatorStatus());
            }

        } catch (Exception exception) {

            if (gameSession.getVendorId() == 1 && dto.getIsEndRound() == 1) {
                responseVo = this.ppEndRoundForceSuccess(gameSession, null, null, traceId);
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

    private WalletBalanceVo ppEndRoundForceSuccess(GameSession gameSession, WalletBalanceVo responseVo, ResponseEntity<String> apiResponse, String traceId) {

        JsonObject originalJson = new JsonObject();
        JsonObject additionalData = new JsonObject();
        long operatorResponseTimeStamp = System.currentTimeMillis();

        additionalData.addProperty("username", gameSession.getAgentPlayerUsername());
        additionalData.addProperty("currency", gameSession.getCurrencyCode());
        additionalData.addProperty("balance", 0);
        additionalData.addProperty("timestamp", operatorResponseTimeStamp);

        if (responseVo != null) {
            originalJson = new Gson().fromJson(apiResponse.getBody(), JsonObject.class);

        } else {
            originalJson.addProperty("traceId", traceId);
            originalJson.addProperty("status", ResponseCodes.Status.SC_OK.code);
            originalJson.addProperty("message", ResponseCodes.Status.SC_OK.description);

        }

        originalJson.add("data", additionalData);
        String updatedJsonString = new Gson().toJson(originalJson);
        responseVo = new Gson().fromJson(updatedJsonString, WalletBalanceVo.class);

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
