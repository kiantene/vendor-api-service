package com.nextgen.gameaggregator.operator.sport.unsettle;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameRepository;
import com.nextgen.gameaggregator.service.*;
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
public class SportUnsettleAction {
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private RequestService requestService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private AgentPlayerRepository agentPlayerRepository;
    @Autowired
    private CurrencyConversionService currencyConversionService;
    @Autowired
    private VendorGameRepository vendorGameRepository;
    @Autowired
    private BetResultRetryLogService betResultRetryLogService;

    public WalletBalanceVo call(String traceId, BetInformation betInformation, HttpRequestLog httpRequestLog, VendorCurrency vendorCurrency) throws VendorCurrencyNotSupportException,
            InvalidAgentApiCredentialException, InvalidOperatorResponseException {

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo = null;
        ResponseCodes.Status defaultResponses = ResponseCodes.Status.SC_OK;
        Integer agentId = betInformation.getAgentId();

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredential.getCallbackUrl();

        String gameCode = vendorGameRepository.findByIdAndStatus(betInformation.getVendorGameId(), 1).getCode();

        AgentPlayer agentPlayer = agentPlayerRepository.findById(betInformation.getAgentPlayerId()).orElse(null);
        SportUnsettleDto dto = this.generateSportUnsettleDto(traceId, agentPlayer.getUsername(), vendorCurrency.getCurrency().getCode(), betInformation, gameCode);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);
        headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            httpRequestLog.setOperatorStart(startTime);

            String jsonApiResponse = new Gson().toJson(dto);
            httpRequestLog.setOperatorData(jsonApiResponse);
            httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.SPORT_UNSETTLE);
        }

        ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.SPORT_UNSETTLE)
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

        requestService.createRequestLogVo(EndPoints.SPORT_UNSETTLE, apiUrl, dto, apiResponse, headerMap, startTime, endTime, this.getClass().getPackage().getName(), profilesActive);

        try {
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
            //TODO TEMPORARY RESPONSES INVALID RESPONSES
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
                betResultRetryLogService.create(httpRequestLog, vendorCurrency.getVendorId(), agentPlayer.getAgentId(), betInformation, EndPoints.SPORT_UNSETTLE);
            }
        }

        return responseVo;
    }

    private SportUnsettleDto generateSportUnsettleDto(String traceId, String agentPlayerUsername, String currencyCode, BetInformation betInformation, String gameCode) {
        SportUnsettleDto sportUnsettleDto = new SportUnsettleDto();
        sportUnsettleDto.setTraceId(traceId);
        sportUnsettleDto.setBetId(betInformation.getBetId());
        sportUnsettleDto.setTransactionId(betInformation.getInternalTransactionId());
        sportUnsettleDto.setUsername(agentPlayerUsername);
        sportUnsettleDto.setCurrency(currencyCode);
        sportUnsettleDto.setExternalTransactionId(betInformation.getExternalTransactionId());
        sportUnsettleDto.setRoundId(betInformation.getRoundId());
        sportUnsettleDto.setTimestamp(betInformation.getVendorBetTime());
        sportUnsettleDto.setGameCode(gameCode);

        return sportUnsettleDto;
    }
}
