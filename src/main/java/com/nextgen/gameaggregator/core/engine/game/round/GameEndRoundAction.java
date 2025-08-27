package com.nextgen.gameaggregator.core.engine.game.round;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.HttpResponseStatusCodeException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultDto;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.Vendors;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GameEndRoundAction {
    // Reusable WebClient for better performance
    private WebClient webClient;
    private final RequestService requestService;
    private final AgentApiCredentialService agentApiCredentialService;
    private final AuthenticationService authenticationService;
    private final CurrencyConversionService currencyConversionService;
    private final BetResultRetryLogService betResultRetryLogService;
    private final Set<Integer> skipVendorList;
    private final AgentApiVersionService agentApiVersionService;

    @Autowired
    public GameEndRoundAction(RequestService requestService,
                              AgentApiCredentialService agentApiCredentialService,
                              AuthenticationService authenticationService,
                              CurrencyConversionService currencyConversionService,
                              BetResultRetryLogService betResultRetryLogService,
                              AgentApiVersionService agentApiVersionService) {

        this.requestService = requestService;
        this.agentApiCredentialService = agentApiCredentialService;
        this.authenticationService = authenticationService;
        this.currencyConversionService = currencyConversionService;
        this.betResultRetryLogService = betResultRetryLogService;
        this.agentApiVersionService = agentApiVersionService;

        this.skipVendorList = new HashSet<>(Set.of(
                Vendors.PGSOFT.getId(),
                Vendors.SPADEGAMING.getId()
        ));
    }

    @PostConstruct
    private void initWebClient() {
        // Configure connection provider for better performance
        ConnectionProvider connectionProvider = ConnectionProvider.builder("endround-web-client-pool")
                .maxConnections(500)                            // Max connections in pool
                .maxIdleTime(Duration.ofSeconds(30))            // Close idle connections after 20s
                .maxLifeTime(Duration.ofSeconds(60))            // Max connection lifetime 60s
                .pendingAcquireTimeout(Duration.ofSeconds(10))  // Timeout waiting for connection
                .evictInBackground(Duration.ofSeconds(60))      // Background cleanup interval
                .metrics(true)
                .build();

        // Configure HTTP client with timeouts and connection settings
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)  // 2s connection timeout
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS))   // 5s read timeout
                                .addHandlerLast(new WriteTimeoutHandler(3, TimeUnit.SECONDS))); // 5s write timeout

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB buffer
                .build();
    }

    public Mono<WalletBalanceVo> callProcessEndRound(String traceId,
                                    Integer agentId,
                                    String agentPlayerUsername,
                                    GameSession gameSession,
                                    SettledBet settledBet,
                                    ResultType resultType,
                                    BigDecimal fromVendorConversionRate,
                                    BigDecimal toVendorConversionRate,
                                    LogContext logContext)
            throws InvalidAgentApiCredentialException {

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String baseUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

        WalletBetResultDto dto = this.newWalletBetResultDto(traceId, agentPlayerUsername, gameSession, settledBet, resultType);
        overwriteIsEndRound(agentId, settledBet.getVendorId(), settledBet.getIsEndRound(), dto);
        currencyConversionService.doCurrencyConversionRateFromVendorForBetResult(dto, fromVendorConversionRate);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        String apiUrl = baseUrl + EndPoints.WALLET_BET_RESULT;

        return processEndRoundAsync(
                apiUrl,
                agentApiCredential.getApiKey(),
                signature,
                dto,
                settledBet,
                toVendorConversionRate,
                logContext
        );
    }

    public Mono<WalletBalanceVo> processEndRoundAsync(String apiUrl, String apiKey,
                                                      String signature,
                                                      WalletBetResultDto dto,
                                                      SettledBet settledBet,
                                                      BigDecimal toVendorConversionRate,
                                                      LogContext logContext) {
        logStart(logContext, apiUrl, dto);
        return webClient
                .post()
                .uri(apiUrl)
                .header(EndPoints.HEADER_API_KEY, apiKey)
                .header(EndPoints.HEADER_SIGNATURE, signature)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .doOnNext(apiResponse -> logEnd(logContext))
                .flatMap(apiResponse -> processResponse(apiResponse, dto, toVendorConversionRate, logContext))
                .onErrorResume(exception -> handleErrorWithRetry(exception, dto, settledBet, logContext))
                .doFinally(signalType -> logEnd(logContext));
    }

    private Mono<WalletBalanceVo> processResponse(ResponseEntity<String> apiResponse,
                                                  WalletBetResultDto dto,
                                                  BigDecimal toVendorConversionRate,
                                                  LogContext logContext) {
        return Mono.fromCallable(() -> {
            if (apiResponse.getStatusCode().isError()) {
                throw new HttpResponseStatusCodeException("HTTP status Code Error");
            }

            logContext.setApiResponse(apiResponse.getBody());
            WalletBalanceVo responseVo = new Gson().fromJson(apiResponse.getBody(), WalletBalanceVo.class);

            if (responseVo == null || !ResponseCodes.Status.SC_OK.equals(responseVo.getStatus())) {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
            }

            RequestService.validateResponse(responseVo);
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, toVendorConversionRate);

            return responseVo;
        }).subscribeOn(Schedulers.boundedElastic()); // Handle potentially blocking operations
    }

    private Mono<WalletBalanceVo> handleErrorWithRetry(Throwable exception,
                                                       WalletBetResultDto dto,
                                                       SettledBet settledBet,
                                                       LogContext logContext) {
        return Mono.fromCallable(() -> {
            logContext.setException(new Exception(exception));
            WalletBalanceVo fallbackResponse = this.forceSuccess(dto, settledBet.getBalance());

            betResultRetryLogService.create(
                    new Gson().toJson(dto),
                    settledBet.getVendorId(),
                    settledBet.getAgentId(),
                    settledBet.getBetId(),
                    settledBet.getRoundId(),
                    settledBet.getInternalTransactionId(),
                    EndPoints.WALLET_BET_RESULT
            );

            return fallbackResponse;
        }).subscribeOn(Schedulers.boundedElastic()); // Handle potentially blocking DB operation
    }

    private WalletBalanceVo forceSuccess(WalletBetResultDto dto, BigDecimal balance) {

        WalletBalanceVo responseVo = new WalletBalanceVo();
        WalletBalanceVo.ResponseData data = new WalletBalanceVo.ResponseData();
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        data.setUsername(dto.getUsername());
        data.setCurrency(dto.getCurrency());
        data.setBalance(balance);
        data.setTimestamp(System.currentTimeMillis());

        responseVo.setTraceId(dto.getTraceId());
        responseVo.setStatus(ResponseCodes.Status.SC_OK);
        responseVo.setMessage(ResponseCodes.Status.SC_OK.description);
        responseVo.setData(data);

        return responseVo;
    }

    private WalletBetResultDto newWalletBetResultDto(String traceId,
                                                     String agentPlayerUsername,
                                                     GameSession gameSession,
                                                     SettledBet settledBet,
                                                     ResultType resultType) {

        // add conversion rate when sending all the figures to operator
        BigDecimal betAmount         = sanitize(settledBet.getBetAmount());
        BigDecimal effectiveTurnover = sanitize(settledBet.getEffectiveTurnover());
        BigDecimal winAmount         = sanitize(settledBet.getWinAmount());
        BigDecimal winLossAmount     = sanitize(settledBet.getWinLoss());
        BigDecimal jackpotAmount     = sanitize(settledBet.getJackpotAmount());

        WalletBetResultDto dto = new WalletBetResultDto();
        dto.setTraceId(traceId);
        dto.setUsername(agentPlayerUsername);
        dto.setBetId(settledBet.getBetId());
        dto.setTransactionId(settledBet.getInternalTransactionId());
        dto.setExternalTransactionId(settledBet.getExternalTransactionId());
        dto.setRoundId(settledBet.getRoundId());
        dto.setBetAmount(betAmount);
        dto.setWinAmount(winAmount);
        dto.setEffectiveTurnover(effectiveTurnover);
        dto.setJackpotAmount(jackpotAmount);
        dto.setWinLoss(winLossAmount);
        dto.setResultType(resultType);
        dto.setIsFreespin(settledBet.getIsFreespin());
        dto.setCurrency(gameSession.getCurrencyCode());
        dto.setToken(settledBet.getGameSessionToken());
        dto.setGameCode(gameSession.getGameCode());
        dto.setBetTime(settledBet.getVendorBetTime());
        dto.setSettledTime(settledBet.getVendorSettleTime());
        dto.setIsEndRound(BetStatus.UNSETTLED.isValueOf(settledBet.getStatus()) ? 0 : 1);

        return dto;
    }

    private void overwriteIsEndRound(Integer agentId, Integer vendorId, Integer isEndRound, WalletBetResultDto dto) {
        Integer agentApiVersion = agentApiVersionService.getAgentApiVersion(agentId);

        if (isEndRound != null && agentApiVersion == 2 && this.skipVendorList.contains(vendorId)) {
            //if isEndRound configure not empty from DTO, and agentApiVersion is 2, and is PGSOFT and SPADEGAMING then set the isEndRound value
            dto.setIsEndRound(isEndRound);
        }
    }

    private BigDecimal sanitize(BigDecimal v) {
        return v == null ? null : this.stripZeroToString(v);
    }

    private BigDecimal stripZeroToString(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }

    private void logStart(LogContext logContext, String url, Object body) {
        logContext.setApiStart(System.currentTimeMillis());
        logContext.setApiUrl(url);
        logContext.setApiBody(body);
    }

    private void logEnd(LogContext logContext) {
        if (logContext != null && logContext.getApiEnd() == 0) {
            logContext.setEnd(System.currentTimeMillis());
        }
    }
}
