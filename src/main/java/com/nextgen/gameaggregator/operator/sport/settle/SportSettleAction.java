package com.nextgen.gameaggregator.operator.sport.settle;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;

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
public class SportSettleAction {
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
    private AgentPlayerRepository agentPlayerRepository;

    public WalletBalanceVo call(String traceId, SportUnsettledBetCouchbase sportUnsettledBetCouchbase, BetInformation betInformation, HttpRequestLog httpRequestLog) throws VendorCurrencyNotSupportException, InvalidAgentApiCredentialException, InvalidOperatorResponseException {

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo;
        Integer agentId = sportUnsettledBetCouchbase.getAgentId();

        VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportUnsettledBetCouchbase.getVendorId(), sportUnsettledBetCouchbase.getCurrencyId());
        BigDecimal fromVendorConversionRate = vendorCurrency.getFromVendorRate();
        BigDecimal toVendorConversionRate = vendorCurrency.getToVendorRate();

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredential.getCallbackUrl();

        AgentPlayer agentPlayer = agentPlayerRepository.findById(sportUnsettledBetCouchbase.getAgentPlayerId()).orElse(null);
        SportSettleDto dto = this.newSportSettleDto(traceId, agentPlayer.getUsername(), vendorCurrency.getCurrency().getCode(), betInformation);
        dto.setBetAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(dto.getBetAmount(), fromVendorConversionRate));

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            httpRequestLog.setOperatorStart(startTime);

            String jsonApiResponse = new Gson().toJson(dto);
            httpRequestLog.setOperatorData(jsonApiResponse);
            httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.SPORT_SETTLE);
        }

        ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.SPORT_SETTLE)
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

        requestService.createRequestLogVo(EndPoints.SPORT_SETTLE, apiUrl, dto, apiResponse, headerMap, startTime, endTime, this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
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
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

            BigDecimal balance = responseVo.getData().getBalance();

            boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
            if (isNegativeBalance) {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            }

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException | ResponseNotMatchRequestException invalidResponseException) {
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            Integer operatorStatus = invalidOperatorResponseException.getOperatorStatus();
            throw new InvalidOperatorResponseException(operatorStatus);

        } catch (Exception exception) {
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);

        }

        return responseVo;
    }

    private SportSettleDto newSportSettleDto(String traceId, String agentPlayerUsername, String currencyCode, BetInformation betInformation) {
        BigDecimal betAmount = new BigDecimal(betInformation.getBetAmount().stripTrailingZeros().toPlainString());
        BigDecimal winAmount = new BigDecimal(betInformation.getWinAmount().stripTrailingZeros().toPlainString());
        BigDecimal winLoss = new BigDecimal(betInformation.getWinLoss().stripTrailingZeros().toPlainString());

        SportSettleDto sportSettleDto = new SportSettleDto();
        sportSettleDto.setTraceId(traceId);
        sportSettleDto.setBetId(betInformation.getBetId());
        sportSettleDto.setTransactionId(betInformation.getInternalTransactionId());
        sportSettleDto.setUsername(agentPlayerUsername);
        sportSettleDto.setCurrency(currencyCode);
        sportSettleDto.setExternalTransactionId(betInformation.getVendorBetId());
        sportSettleDto.setBetAmount(betAmount);
        sportSettleDto.setRoundId(betInformation.getRoundId());
        sportSettleDto.setTimestamp(betInformation.getVendorBetTime());
        sportSettleDto.setGameCode("sports");
        sportSettleDto.setWinAmount(winAmount);
        sportSettleDto.setWinLoss(winLoss);
        sportSettleDto.setEffectiveTurnover(betAmount);

        return sportSettleDto;
    }
}
