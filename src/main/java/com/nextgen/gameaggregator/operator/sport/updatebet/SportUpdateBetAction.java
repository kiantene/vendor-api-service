package com.nextgen.gameaggregator.operator.sport.updatebet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameRepository;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;
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
public class SportUpdateBetAction {

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private CurrencyConversionService currencyConversionService;
    @Autowired
    private RequestService requestService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorGameRepository vendorGameRepository;
    @Autowired
    private BetResultRetryLogService betResultRetryLogService;

    public WalletBalanceVo call(String traceId, GameSession gameSession, SportUnsettledBetCouchbase betInformation, HttpRequestLog httpRequestLog, VendorCurrency vendorCurrency) throws VendorCurrencyNotSupportException, InvalidAgentApiCredentialException, InvalidOperatorResponseException {

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo = null;
        ResponseCodes.Status defaultResponses = ResponseCodes.Status.SC_OK;
        Integer agentId = gameSession.getAgentId();

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredential.getCallbackUrl();

        String gameCode = vendorGameRepository.findById(betInformation.getVendorGameId()).map(VendorGame::getCode).orElse(null);

        SportUpdateBetDto dto = this.newSportUpdateBetDto(traceId, gameSession, betInformation, gameCode, vendorCurrency);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);
        headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            httpRequestLog.setOperatorStart(startTime);

            String jsonApiResponse = new Gson().toJson(dto);
            httpRequestLog.setOperatorData(jsonApiResponse);
            httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.SPORT_UPDATE_BET);
        }

        ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.SPORT_UPDATE_BET)
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

        requestService.createRequestLogVo(EndPoints.SPORT_UPDATE_BET, apiUrl, dto, apiResponse, headerMap, startTime, endTime, this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
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

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException |
                 InvalidResponseException |
                 ResponseNotMatchRequestException invalidResponseException) {

            defaultResponses = ResponseCodes.Status.SC_INVALID_RESPONSE;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //Integer operatorStatus = invalidOperatorResponseException.getOperatorStatus();
            defaultResponses = ResponseCodes.Status.SC_INVALID_RESPONSE;

        } catch (Exception exception) {
            defaultResponses = ResponseCodes.Status.SC_UNKNOWN_ERROR;

        } finally {
            if (defaultResponses == ResponseCodes.Status.SC_OK) {
                //do nothing if success

            } else {
                WalletBalanceVo.ResponseData responseData = new WalletBalanceVo.ResponseData();
                //create betResultRetryLog info for retry send to operator
                responseVo.setData(responseData);
                responseVo.getData().setBalance(BigDecimal.ZERO);
                responseVo.setStatus(defaultResponses);
                responseVo.setTraceId(traceId);
                betResultRetryLogService.create(httpRequestLog, vendorCurrency.getVendorId(), betInformation.getAgentId(), betInformation, EndPoints.SPORT_UPDATE_BET);
            }

        }

        return responseVo;
    }

    private SportUpdateBetDto newSportUpdateBetDto(String traceId, GameSession gameSession, SportUnsettledBetCouchbase betInformation, String gameCode, VendorCurrency vendorCurrency) {
        BigDecimal betAmount = new BigDecimal(betInformation.getBetAmount().stripTrailingZeros().toPlainString());
        BigDecimal newBetAmount = new BigDecimal(betInformation.getNewBetAmount().stripTrailingZeros().toPlainString());
        BigDecimal creditAmount = betAmount.subtract(newBetAmount);

        SportUpdateBetDto sportUpdateBetDto = new SportUpdateBetDto();
        sportUpdateBetDto.setTraceId(traceId);
        sportUpdateBetDto.setBetId(betInformation.getBetId());
        sportUpdateBetDto.setTransactionId(betInformation.getInternalTransactionId());
        sportUpdateBetDto.setUsername(gameSession.getAgentPlayerUsername());
        sportUpdateBetDto.setCurrency(gameSession.getCurrencyCode());
        sportUpdateBetDto.setExternalTransactionId(betInformation.getVendorBetId());
        sportUpdateBetDto.setRoundId(betInformation.getRoundId());
        sportUpdateBetDto.setTimestamp(betInformation.getVendorBetTime());
        sportUpdateBetDto.setGameCode(gameCode);

        sportUpdateBetDto.setBetAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(betAmount, vendorCurrency.getFromVendorRate()));
        sportUpdateBetDto.setNewBetAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(newBetAmount, vendorCurrency.getFromVendorRate()));
        sportUpdateBetDto.setCreditAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(creditAmount, vendorCurrency.getFromVendorRate()));

        return sportUpdateBetDto;
    }
}
