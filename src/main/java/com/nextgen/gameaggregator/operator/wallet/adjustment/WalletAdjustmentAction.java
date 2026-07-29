package com.nextgen.gameaggregator.operator.wallet.adjustment;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.*;
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
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class WalletAdjustmentAction {
    @Autowired
    AgentApiCredentialService agentApiCredentialService;
    @Autowired
    AuthenticationService authenticationService;
    @Autowired
    RequestService requestService;
    @Autowired
    CurrencyConversionService currencyConversionService;
    @Autowired
    BetResultRetryLogService betResultRetryLogService;
    @Autowired
    HttpService httpService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    public WalletBalanceVo call(String traceId, Integer agentId, GameSession gameSession, BetInformation betInformation, HttpRequestLog httpRequestLog, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate)
            throws InvalidOperatorResponseException, InvalidAgentApiCredentialException, InsufficientBalanceException {

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo = null;

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

        WalletAdjustmentDto dto = this.newWalletAdjustmentDto(traceId, gameSession, betInformation);
        dto.setAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(dto.getAmount(), fromVendorConversionRate));

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);
        headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());

        long startTime = System.currentTimeMillis();
        httpRequestLog.setOperatorStart(startTime);

        String jsonApiResponse = new Gson().toJson(dto);
        httpRequestLog.setOperatorData(jsonApiResponse);
        httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.WALLET_ADJUSTMENT);

        boolean isError = false;
        ResponseCodes.Status operatorStatus = ResponseCodes.Status.SC_UNKNOWN_ERROR;
        // if match useStub and username prefix will skip call to stub
        if (requestService.shouldSkipStubCall(dto.getUsername())) {
            return requestService.responseOperatorSub();
        }

        try {
            ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.WALLET_ADJUSTMENT)
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

            requestService.createRequestLogVo(
                    EndPoints.WALLET_ADJUSTMENT, apiUrl, dto, apiResponse, headerMap, startTime, endTime,
                    this.getClass().getPackage().getName(), profilesActive);


            log.info("Response [" + apiUrl + EndPoints.WALLET_ADJUSTMENT + "]: " + apiResponse);

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

            // 4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

            // 5. add conversion rate when returning the balance to vendor
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

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

        } finally {
            if (isError) {
                //GA-12667 add force success to vendor and retry to operator.
                responseVo = this.processForceSuccess(gameSession, traceId, betInformation);

                //log error
                httpService.logError(httpRequestLog, new InvalidOperatorResponseException(operatorStatus.description));
                //send retry to operator
                betResultRetryLogService.create(httpRequestLog.getOperatorData(),
                        gameSession.getVendorId(),
                        betInformation.getAgentId(),
                        betInformation.getBetId(),
                        betInformation.getRoundId(),
                        betInformation.getInternalTransactionId(),
                        EndPoints.WALLET_ADJUSTMENT);

            }

        }
        return responseVo;
    }

    private WalletAdjustmentDto newWalletAdjustmentDto(String traceId, GameSession gameSession, BetInformation betInformation) {
        BigDecimal amount = new BigDecimal(betInformation.getWinAmount().stripTrailingZeros().toPlainString());

        WalletAdjustmentDto walletAdjustmentDto = new WalletAdjustmentDto();
        walletAdjustmentDto.setTraceId(traceId);
        walletAdjustmentDto.setTransactionId(betInformation.getInternalTransactionId());
        walletAdjustmentDto.setUsername(gameSession.getAgentPlayerUsername());
        walletAdjustmentDto.setCurrency(gameSession.getCurrencyCode());
        walletAdjustmentDto.setExternalTransactionId(betInformation.getVendorBetId());
        walletAdjustmentDto.setAmount(amount);
        walletAdjustmentDto.setGameCode(gameSession.getGameCode());
        walletAdjustmentDto.setRoundId(betInformation.getRoundId());
        walletAdjustmentDto.setTimestamp(betInformation.getVendorBetTime());

        return walletAdjustmentDto;
    }

    private WalletBalanceVo processForceSuccess(GameSession gameSession, String traceId, BetInformation settledBet) {

        WalletBalanceVo responseVo = new WalletBalanceVo();
        WalletBalanceVo.ResponseData data = new WalletBalanceVo.ResponseData();
        BigDecimal balance = (settledBet.getBalance() == null) ? BigDecimal.ZERO : settledBet.getBalance();

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
}
