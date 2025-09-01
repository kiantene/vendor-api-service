package com.nextgen.gameaggregator.core.engine.game.round;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EndRoundProcessor {
    private static final Gson GSON = new Gson();
    private final SettledBetService settledBetService;
    private final UnsettledBetService unsettledBetService;
    private final VendorPlayerService vendorPlayerService;
    private final KafkaService kafkaService;
    private final VendorGameService vendorGameService;
    private final VendorCurrencyService vendorCurrencyService;
    private final AgentApiVersionService agentApiVersionService;
    private final GameEndRoundAction gameEndRoundAction;
    private final Set<Integer> skipVendorList = new HashSet<>(Set.of(
            Vendors.PGSOFT.getId(),
            Vendors.SPADEGAMING.getId()
    ));

    @Async("endRoundExecutor")
    public CompletableFuture<Void> process(SettledBet settledBet,
                                           UnsettledBet unsettledBet,
                                           EndRoundSettledBet endRoundSettledBet,
                                           GameSession gameSession) {
        String traceId = UUID.randomUUID().toString();
        LogContext logContext = initLogging(settledBet, endRoundSettledBet.getAgentPlayerUsername());

        try {
            EndRoundResult result = processEndRoundInternal(traceId, settledBet, unsettledBet, endRoundSettledBet, gameSession);
            handleSuccessfulProcessing(settledBet, gameSession, result, logContext);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            handleError(logContext, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private LogContext initLogging(SettledBet settledBet, String username) {
        LogContext logContext = new LogContext();
        logContext.setLogGroup("game");
        logContext.setType("endround");
        logContext.setAgentId(settledBet.getAgentId());
        logContext.setVendorId(settledBet.getVendorId());
        logContext.setUsername(username);
        logContext.setStart(System.currentTimeMillis());
        logContext.setBody(settledBet.getRawData());
        return logContext;
    }

    private EndRoundResult processEndRoundInternal(String traceId,
                                                   SettledBet settledBet,
                                                   UnsettledBet unsettledBet,
                                                   EndRoundSettledBet endRoundSettledBet,
                                                   GameSession gameSession) throws
            InvalidPlayerException, GameNotSupportedException, VendorCurrencyNotSupportException {

        // Get required entities
        VendorPlayer vendorPlayer = getVendorPlayer(settledBet.getVendorPlayerId());
        String agentPlayerUsername = endRoundSettledBet.getAgentPlayerUsername();
        VendorCurrency vendorCurrency = getCurrencyConversionRate(settledBet.getVendorId(), settledBet.getCurrencyId(), traceId);

        // Save settled bet
        settledBetService.save(settledBet, settledBet.getRawData());

        // Process bet history
        processBetHistory(settledBet, gameSession, agentPlayerUsername, vendorPlayer.getUsername(), vendorCurrency);

        // Clean up unsettled bet
        unsettledBetService.delete(unsettledBet);

        return new EndRoundResult(
                traceId,
                endRoundSettledBet.getAgentId(),
                agentPlayerUsername,
                vendorCurrency);
    }

    private void handleSuccessfulProcessing(SettledBet settledBet, GameSession gameSession, EndRoundResult result, LogContext logContext) {

        Integer agentApiVersion = agentApiVersionService.getAgentApiVersion(result.agentId());
        boolean skipSendToOperator = agentApiVersion == 2 && this.skipVendorList.contains(settledBet.getVendorId());
        if (skipSendToOperator) { // skip sending to operator if they are running version 2 of the api
            logContext.setUrl(this.getClass().getSimpleName());
            logContext.setResponse("Skip Operator API");
            logContext.setEnd();
            log.info(logContext.toJson()); // print log here since we skip operator api call
            return;
        }

        VendorCurrency vendorCurrency = result.vendorCurrency();
        this.notifyEndRoundProcess(
                result,
                gameSession,
                settledBet,
                vendorCurrency.getFromVendorRate(),
                vendorCurrency.getToVendorRate(),
                logContext
        );
    }

    private VendorPlayer getVendorPlayer(Long vendorPlayerId) throws InvalidPlayerException {
        return vendorPlayerService.getByVendorPlayerId(vendorPlayerId, null);
    }

    private boolean isPreProcessingRequired(Integer vendorGameId) throws GameNotSupportedException {
        VendorGame vendorGame = vendorGameService.getByVendorGameId(vendorGameId);
        return vendorGame.getBetDataPreprocessing() == 1;
    }

    public VendorCurrency getCurrencyConversionRate(Integer vendorId, Integer currencyId, String traceId) throws VendorCurrencyNotSupportException {
        BigDecimal defaultConversionRateAsOne = BigDecimal.ONE;
        VendorCurrency vendorCurrency = vendorCurrencyService.findByVendorIdAndCurrencyId(vendorId, currencyId);

        //if unset or zero for FromVendorRate, will be set as 1
        if (vendorCurrency.getFromVendorRate() == null || vendorCurrency.getFromVendorRate().compareTo(BigDecimal.ZERO) == 0) {
            vendorCurrency.setFromVendorRate(defaultConversionRateAsOne);
            log.error("Currency conversion failed, FromVendorRate() is zero or empty for vendorId : " + vendorId + " | currencyId = " + currencyId + " ｜ traceId = " + traceId);
        }

        //if unset or zero for ToVendorRate, will be set as 1
        if (vendorCurrency.getToVendorRate() == null || vendorCurrency.getToVendorRate().compareTo(BigDecimal.ZERO) == 0) {
            vendorCurrency.setToVendorRate(defaultConversionRateAsOne);
            log.error("Currency conversion failed, ToVendorRate() is zero or empty for vendorId : " + vendorId + " | currencyId = " + currencyId + " ｜ traceId = " + traceId);
        }

        return vendorCurrency;
    }

    private void processBetHistory(SettledBet settledBet,
                                   GameSession gameSession,
                                   String agentPlayerUsername,
                                   String vendorPlayerUsername,
                                   VendorCurrency vendorCurrency) throws GameNotSupportedException {
        BetHistory betHistory = new BetHistory(settledBet);

        if (!isPreProcessingRequired(settledBet.getVendorGameId())) {
            processNormalBetHistory(
                    betHistory,
                    agentPlayerUsername,
                    vendorPlayerUsername,
                    gameSession,
                    vendorCurrency,
                    settledBet
            );
        } else {
            processPreprocessingBetHistory(betHistory, agentPlayerUsername, vendorPlayerUsername, vendorCurrency);
        }
    }

    private void processNormalBetHistory(BetHistory betHistory,
                                         String agentPlayerUsername,
                                         String vendorPlayerUsername,
                                         GameSession gameSession,
                                         VendorCurrency vendorCurrency,
                                         SettledBet settledBet) {
        kafkaService.produceBetHistoryV3(
                betHistory,
                gameSession.getProductCode(),
                gameSession.getProductId(),
                gameSession.getProductGameId(),
                agentPlayerUsername,
                vendorPlayerUsername,
                vendorCurrency.getFromVendorRate()
        );

        kafkaService.produceBetHistoryUncap(settledBet, gameSession.getProductCode(), gameSession.getProductId(), gameSession.getProductGameId(),
                agentPlayerUsername, vendorPlayerUsername, vendorCurrency.getFromVendorRate());
    }

    private void processPreprocessingBetHistory(BetHistory betHistory, 
                                                String agentPlayerUsername,
                                                String vendorPlayerUsername,
                                                VendorCurrency vendorCurrency) {
        kafkaService.producePreprocessingBetHistory(
                betHistory,
                agentPlayerUsername,
                vendorPlayerUsername,
                vendorCurrency.getFromVendorRate()
        );
    }

    private void notifyEndRoundProcess(EndRoundResult result,
                                       GameSession gameSession,
                                       SettledBet settledBet,
                                       BigDecimal fromVendorConversionRate,
                                       BigDecimal toVendorConversionRate,
                                       LogContext logContext) {

        String operatorResultType = "END";
        try {
            ResultType resultType = ResultType.getResultTypeByDescription(operatorResultType);

            // Call reactive operation with proper subscription handling
            gameEndRoundAction.callProcessEndRound(
                        result.traceId(),
                        result.agentId(),
                        result.agentPlayerUsername(),
                        gameSession,
                        settledBet,
                        resultType,
                        fromVendorConversionRate,
                        toVendorConversionRate,
                        logContext
                    )
                    .subscribe(walletResult -> { // Success handler - called when operation completes successfully
                        GeneralVo successVo = new GeneralVo();
                        successVo.setResponseCode(ResponseCode.SUCCESS);
                        logContext.setResponse(GSON.toJson(successVo));
                        logContext.setEnd();
                        log.info(logContext.toJson());
                    },
                    error -> handleError(logContext, new Exception(error))
            );

        } catch (Exception e) {
            handleError(logContext, e);
        }
    }

    private void handleError(LogContext logContext, Exception ex) {
        GeneralVo errorVo = new GeneralVo();
        errorVo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
        logContext.setResponse(GSON.toJson(errorVo));
        logContext.setException(ex);
        logContext.setEnd();
        log.error(logContext.toJson());
    }

    private record EndRoundResult(
            String traceId,
            Integer agentId,
            String agentPlayerUsername,
            VendorCurrency vendorCurrency) {
    }
}
