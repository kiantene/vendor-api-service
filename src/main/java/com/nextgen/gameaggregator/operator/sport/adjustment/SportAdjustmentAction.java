package com.nextgen.gameaggregator.operator.sport.adjustment;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
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
public class SportAdjustmentAction {
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AgentPlayerRepository agentPlayerRepository;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private CurrencyConversionService currencyConversionService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private RequestService requestService;
    @Autowired
    private BetResultRetryLogService betResultRetryLogService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    public WalletBalanceVo call(String traceId, Integer agentId, BetInformation betInformation, HttpRequestLog httpRequestLog)
            throws InvalidOperatorResponseException, InvalidAgentApiCredentialException, InsufficientBalanceException, VendorCurrencyNotSupportException {

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo = null;
        ResponseCodes.Status defaultResponses = ResponseCodes.Status.SC_OK;

        VendorCurrency vendorCurrency = vendorService.findVendorCurrency(betInformation.getVendorId(), betInformation.getCurrencyId());
        BigDecimal fromVendorConversionRate = vendorCurrency.getFromVendorRate();
        BigDecimal toVendorConversionRate = vendorCurrency.getToVendorRate();

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredential.getCallbackUrl();

        AgentPlayer agentPlayer = agentPlayerRepository.findById(betInformation.getAgentPlayerId()).orElse(null);
        SportAdjustmentDto dto = this.newSportAdjustmentDto(traceId, betInformation, agentPlayer.getUsername(), vendorCurrency.getCurrency().getCode());
        dto.setAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(dto.getAmount(), fromVendorConversionRate));

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);
        headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            httpRequestLog.setOperatorStart(startTime);

            String jsonApiResponse = new Gson().toJson(dto);
            httpRequestLog.setOperatorData(jsonApiResponse);
            httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.SPORT_ADJUSTMENT);
        }

        try {
            ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.SPORT_ADJUSTMENT)
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

            if (apiResponse != null) {
                httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());

            }
            httpRequestLog.setOperatorEnd(endTime);

            if (httpRequestLog != null) {
                if (apiResponse != null) {
                    httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());
                }
                httpRequestLog.setOperatorEnd(endTime);
            }

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
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

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
            //defaultResponses = invalidOperatorResponseException.getOperatorStatus();
            defaultResponses = ResponseCodes.Status.SC_INVALID_RESPONSE;

        } catch (Exception exception) {
            defaultResponses = ResponseCodes.Status.SC_UNKNOWN_ERROR;

        } finally {
            if (defaultResponses == ResponseCodes.Status.SC_OK) {
                //do nothing if success

            } else {
                //create betResultRetryLog info for retry send to operator
                WalletBalanceVo.ResponseData responseData = new WalletBalanceVo.ResponseData();
                //create betResultRetryLog info for retry send to operator
                responseVo.setData(responseData);
                responseVo.getData().setBalance(BigDecimal.ZERO);
                responseVo.setStatus(defaultResponses);
                responseVo.setTraceId(traceId);
                betResultRetryLogService.create(httpRequestLog, vendorCurrency.getVendorId(), agentPlayer.getAgentId(), betInformation, EndPoints.SPORT_ADJUSTMENT);
            }

        }

        return responseVo;
    }

    private SportAdjustmentDto newSportAdjustmentDto(String traceId, BetInformation betInformation, String agentPlayerUsername, String currencyCode) {
        BigDecimal amount = new BigDecimal(betInformation.getWinAmount().stripTrailingZeros().toPlainString());

        SportAdjustmentDto sportAdjustmentDto = new SportAdjustmentDto();
        sportAdjustmentDto.setTraceId(traceId);
        sportAdjustmentDto.setTransactionId(betInformation.getInternalTransactionId());
        sportAdjustmentDto.setUsername(agentPlayerUsername);
        sportAdjustmentDto.setCurrency(currencyCode);
        sportAdjustmentDto.setExternalTransactionId(betInformation.getVendorBetId());
        sportAdjustmentDto.setAmount(amount);
        sportAdjustmentDto.setRoundId(betInformation.getRoundId());
        sportAdjustmentDto.setTimestamp(betInformation.getVendorBetTime());

        return sportAdjustmentDto;
    }
}
