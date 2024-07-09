package com.nextgen.gameaggregator.operator.sport.refund;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.SportsBaseAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameRepository;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
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
public class SportRefundAction extends SportsBaseAction {
    private final AgentApiCredentialService agentApiCredentialService;
    private final AuthenticationService authenticationService;
    private final RequestService requestService;
    private final CurrencyConversionService currencyConversionService;
    private final VendorGameRepository vendorGameRepository;
    private final BetResultRetryLogService betResultRetryLogService;
    private final CurrencyService currencyService;
    @Value("${spring.profiles.active}")
    private String profilesActive;

    public SportRefundAction(AgentApiCredentialService agentApiCredentialService,
                             AuthenticationService authenticationService,
                             RequestService requestService,
                             CurrencyConversionService currencyConversionService,
                             VendorGameRepository vendorGameRepository,
                             BetResultRetryLogService betResultRetryLogService,
                             CurrencyService currencyService) {

        this.endpoint = EndPoints.SPORT_REFUND;
        this.requestType = this.getClass().getSimpleName();
        this.agentApiCredentialService = agentApiCredentialService;
        this.authenticationService = authenticationService;
        this.requestService = requestService;
        this.currencyConversionService = currencyConversionService;
        this.vendorGameRepository = vendorGameRepository;
        this.betResultRetryLogService = betResultRetryLogService;
        this.currencyService = currencyService;
    }

    public WalletBalanceVo call(String traceId, SportUnsettledBet betInformation, HttpRequestLog httpRequestLog, VendorCurrency vendorCurrency, AgentPlayer agentPlayer) throws VendorCurrencyNotSupportException,
            InvalidAgentApiCredentialException, InvalidOperatorResponseException, RecordNotFoundException {

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo = null;
        ResponseCodes.Status defaultResponses = ResponseCodes.Status.SC_OK;
        Integer agentId = betInformation.getAgentId();

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

        String gameCode = vendorGameRepository.findById(betInformation.getVendorGameId()).map(VendorGame::getCode).orElse(null);

        Integer currencyId = vendorCurrency.getCurrencyId();
        String currencyCode = vendorCurrency.getVendorCurrencyCode();
        try {
            Currency currency = currencyService.get(currencyId);
            currencyCode = currency.getCode();
        } catch (InvalidCurrencyException invalidCurrencyException) {
            // do nothing to suppress the error
        }

        SportRefundDto dto = this.newSportRefundDto(traceId, agentPlayer.getUsername(), currencyCode, betInformation, gameCode);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);
        headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());

        VendorGame vendorGame = vendorGameRepository.findById(betInformation.getVendorGameId()).orElse(null);
        httpRequestLog.setVendorGameCode(vendorGame.getVendorGameCode());
        httpRequestLog.setOperatorUsername(agentPlayer.getUsername());

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            httpRequestLog.setOperatorStart(startTime);

            String jsonApiResponse = new Gson().toJson(dto);
            httpRequestLog.setOperatorData(jsonApiResponse);
            httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.SPORT_REFUND);
        }

        try {
            ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.SPORT_REFUND)
                    .header(EndPoints.HEADER_SIGNATURE, signature)
                    .header(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                    .toEntity(String.class)
                    .retry(3)
                    .timeout(Duration.ofMillis(EndPoints.SPORTBOOK_TIMEOUT))
                    .block();

            long endTime = System.currentTimeMillis();

            if (httpRequestLog != null) {
                if (apiResponse != null) {
                    httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());
                }
                httpRequestLog.setOperatorEnd(endTime);
            }

            requestService.createRequestLogVo(EndPoints.SPORT_REFUND, apiUrl, dto, apiResponse, headerMap, startTime, endTime, this.getClass().getPackage().getName(), profilesActive);

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response'
            if (apiResponse == null) throw new InvalidOperatorResponseException();
            responseVo = new Gson().fromJson(apiResponse.getBody(), WalletBalanceVo.class);

            if (httpRequestLog != null) {
                httpRequestLog.setOperatorResponse(apiResponse.getBody());
                httpRequestLog.setOperatorResponseStatus(responseVo.getStatus());
            }

            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            RequestService.validateResponse(responseVo);

            //3. validate username and currency
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

            // 5. add conversion rate when returning the balance to vendor
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, vendorCurrency.getToVendorRate());

            BigDecimal balance = responseVo.getData().getBalance();

            boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
            if (isNegativeBalance) {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            }

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException |
                 ResponseNotMatchRequestException invalidResponseException) {
            defaultResponses = ResponseCodes.Status.SC_INVALID_RESPONSE;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //Integer operatorStatus = invalidOperatorResponseException.getOperatorStatus();
            //TODO TEMPORARY RESPONSES INVALID RESPONSES
            defaultResponses = ResponseCodes.Status.SC_INVALID_RESPONSE;

        } catch (Exception exception) {
            if (exception.getMessage().contains("java.util.concurrent.TimeoutException")) {
                httpRequestLog.setErrorMessage(exception.getMessage());
            }
            defaultResponses = ResponseCodes.Status.SC_UNKNOWN_ERROR;

        } finally {
            if (defaultResponses == ResponseCodes.Status.SC_OK) {
                //do nothing if success

            } else {
                responseVo = betResultRetryLogService.processForceSuccess(traceId, agentPlayer.getUsername(), currencyCode, betInformation);
                betResultRetryLogService.create(httpRequestLog.getOperatorData(), vendorCurrency.getVendorId(), agentPlayer.getAgentId(), betInformation.getBetId(), betInformation.getRoundId(), betInformation.getInternalTransactionId(), EndPoints.SPORT_REFUND);
            }

        }

        return responseVo;
    }

    private SportRefundDto newSportRefundDto(String traceId, String agentPlayerUsername, String currencyCode, SportUnsettledBet betInformation, String gameCode) {
        SportRefundDto sportRefundDto = new SportRefundDto();
        sportRefundDto.setTraceId(traceId);
        sportRefundDto.setUsername(agentPlayerUsername);
        sportRefundDto.setTransactionId(betInformation.getInternalTransactionId());
        sportRefundDto.setExternalTransactionId(betInformation.getExternalTransactionId());
        sportRefundDto.setBetId(betInformation.getBetId());
        sportRefundDto.setRoundId(betInformation.getRoundId());
        sportRefundDto.setGameCode(gameCode);
        sportRefundDto.setCurrency(currencyCode);
        sportRefundDto.setTimestamp(betInformation.getVendorBetTime());

        return sportRefundDto;
    }
}
