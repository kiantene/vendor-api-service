package com.nextgen.gameaggregator.operator.wallet.bet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.core.exception.OperatorNetworkException;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.Vendors;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class WalletBetAction {

    private static final Integer TIMEOUT_BUFFER = 200; // buffer duration for internal processing, set to 200ms
    private final RequestService requestService;
    private final AgentApiCredentialService agentApiCredentialService;
    private final AuthenticationService authenticationService;
    private final VendorService vendorService;
    private final CurrencyConversionService currencyConversionService;
    private final LogContextService logContextService;
    private final Set<Integer> vendorsWithTwoPointFiveSecondTimeout;
    private final Set<Integer> vendorsWithThreePointFiveSecondTimeout;
    private final Set<Integer> vendorsWithFourPointFiveSecondTimeout;
    private final Set<Integer> vendorsWithFourSecondTimeout;
    private final Set<Integer> vendorsWithTwoPointTwoSecondTimeout;
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    public WalletBetAction(RequestService requestService,
                           AgentApiCredentialService agentApiCredentialService,
                           AuthenticationService authenticationService,
                           VendorService vendorService,
                           CurrencyConversionService currencyConversionService,
                           LogContextService logContextService) {
        this.requestService = requestService;
        this.agentApiCredentialService = agentApiCredentialService;
        this.authenticationService = authenticationService;
        this.vendorService = vendorService;
        this.currencyConversionService = currencyConversionService;
        this.logContextService = logContextService;
        this.vendorsWithTwoPointFiveSecondTimeout = new HashSet<>();
        this.vendorsWithThreePointFiveSecondTimeout = new HashSet<>();
        this.vendorsWithFourPointFiveSecondTimeout = new HashSet<>();
        this.vendorsWithFourSecondTimeout = new HashSet<>();
        this.vendorsWithTwoPointTwoSecondTimeout = new HashSet<>();
        //ambs
        this.vendorsWithTwoPointFiveSecondTimeout.add(38);
        //jili
        this.vendorsWithThreePointFiveSecondTimeout.add(4);
        //koolbet
        this.vendorsWithFourSecondTimeout.add(76);
        //gpk
        this.vendorsWithTwoPointTwoSecondTimeout.addAll(Set.of(45, 46, 49, 52, 54, 75, 85));

    }

    public WalletBalanceVo call(String traceId, GameSession gameSession, BetInformation betInformation, HttpRequestLog httpRequestLog)
            throws InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, VendorCurrencyNotSupportException {

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo;
        Integer agentId = gameSession.getAgentId();

        VendorCurrency vendorCurrency = vendorService.getCurrencyConversionRate(gameSession, traceId);
        BigDecimal fromVendorConversionRate = vendorCurrency.getFromVendorRate();
        BigDecimal toVendorConversionRate = vendorCurrency.getToVendorRate();

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

        WalletBetDto dto = this.newWalletBetDto(traceId, gameSession, betInformation);
        dto.setAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(dto.getAmount(), fromVendorConversionRate));
        //log.info("Request [" + apiUrl + EndPoints.WALLET_BET + "]: " + dto);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);
        headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            httpRequestLog.setOperatorStart(startTime);

            String jsonApiResponse = new Gson().toJson(dto);
            httpRequestLog.setOperatorData(jsonApiResponse);
            httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.WALLET_BET);

        }

        ResponseEntity<String> apiResponse = null;

        // if match useStub and username prefix will skip call to stub
        if (requestService.shouldSkipStubCall(dto.getUsername())) {
            return requestService.responseOperatorSub();
        }

        Integer vendorTimeoutInMillis = getVendorTimeoutInMillis(gameSession);
        AtomicBoolean isTimeout = new AtomicBoolean(false);
        try {
            logContextService.logStart(apiUrl + EndPoints.WALLET_BET, dto);
            apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.WALLET_BET)
                    .header(EndPoints.HEADER_SIGNATURE, signature)
                    .header(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                    .toEntity(String.class)
                    .retry(3)
                    .timeout(Duration.ofMillis(vendorTimeoutInMillis))
                    .onErrorResume(TimeoutException.class, e -> {
                        isTimeout.set(true);
                        return Mono.error(e);
                    })
                    .block();

            long endTime = System.currentTimeMillis();
            logContextService.logEnd(apiResponse);

            if (httpRequestLog != null) {
                if (apiResponse != null) {
                    httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());

                }
                httpRequestLog.setOperatorEnd(endTime);
            }

            if (isTimeout.get()) {
                if (Vendors.isNewFramework(betInformation.getVendorId())) {
                    throw new OperatorNetworkException("Operator timeout", apiUrl, null);
                } else {
                    throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code);
                }
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

            // 4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

            // 5. add conversion rate when returning the balance to vendor
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

            BigDecimal balance = responseVo.getData().getBalance();
            //TODO to be discuss whether should system pre handle negative if
            boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
            if (isNegativeBalance) {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            }

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException |
                 InvalidResponseException |
                 ResponseNotMatchRequestException invalidResponseException) {

            //success receive responses from OPERATOR, but due to invalid httpStatus responses, throw error.
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //RequestService.failResponseLog(requestLogVo, invalidOperatorResponseException);
            Integer operatorStatus = invalidOperatorResponseException.getOperatorStatus();
            throw new InvalidOperatorResponseException(operatorStatus);

        } catch (Exception exception) {

            if (apiResponse != null) {
                //fail receive responses from OPERATOR, but due to invalid httpStatus responses / timeout, throw error.
                if (apiResponse.getStatusCode().isError()) {
                    throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
                }
            }
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
        } finally {
            logContextService.logEnd(apiResponse);
        }
        return responseVo;
    }

    private WalletBetDto newWalletBetDto(String traceId, GameSession gameSession, BetInformation betInformation) {
        BigDecimal amount = new BigDecimal(betInformation.getBetAmount().stripTrailingZeros().toPlainString());

        WalletBetDto walletBetDto = new WalletBetDto();
        walletBetDto.setTraceId(traceId);
        walletBetDto.setBetId(betInformation.getBetId());
        walletBetDto.setTransactionId(betInformation.getInternalTransactionId());
        walletBetDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBetDto.setCurrency(gameSession.getCurrencyCode());
        walletBetDto.setToken(gameSession.getToken());

        /**
         * Jeff: this is legacy code, not sure why it was mapped this way
         * but for vendors running on new framework, it will be mapped correctly.
         *
         * TODO: When we start migrating old vendors to use new framework,
         * TODO: existing operator will be impacted by this change
         */
        Integer vendorId = betInformation.getVendorId();
        if (vendorId != null && Vendors.isNewFramework(vendorId)) { // map correctly
            walletBetDto.setExternalTransactionId(betInformation.getExternalTransactionId());
        } else {
            walletBetDto.setExternalTransactionId(betInformation.getVendorBetId());
        }

        walletBetDto.setAmount(amount);
        walletBetDto.setGameCode(gameSession.getGameCode());
        walletBetDto.setRoundId(betInformation.getRoundId());
        walletBetDto.setTimestamp(betInformation.getVendorBetTime());

        return walletBetDto;
    }

    private Integer operatorTimeoutConfig(GameSession gameSession) {

        if (this.vendorsWithTwoPointFiveSecondTimeout.contains(gameSession.getVendorId())) {
            //operator timeout set to 2.5sec
            return 2500;
        } else if (this.vendorsWithThreePointFiveSecondTimeout.contains(gameSession.getVendorId())) {
            //operator timeout set to 3.5sec
            return 3500;
        } else if (this.vendorsWithFourSecondTimeout.contains(gameSession.getVendorId())) {
            //operator timeout set to 4sec
            return 4000;
        } else if (this.vendorsWithFourPointFiveSecondTimeout.contains(gameSession.getVendorId())) {
            //operator timeout set to 4.5sec
            return 4500;
        } else if (this.vendorsWithTwoPointTwoSecondTimeout.contains(gameSession.getVendorId())) {
            //operator timeout set to 2.2sec
            return 2200;
        }
        //default operator timeout (5sec)
        return EndPoints.TIMEOUT;
    }

    private Integer getVendorTimeoutInMillis(GameSession gameSession) {
        Integer timeout = this.operatorTimeoutConfig(gameSession);
        Integer vendorId = gameSession.getVendorId();
        if (vendorId != null && Vendors.isNewFramework(vendorId)) {
            Vendors vendor = Vendors.fromId(vendorId);
            timeout = vendor.getTimeoutMillis() - TIMEOUT_BUFFER;
        }
        return timeout;
    }
}
