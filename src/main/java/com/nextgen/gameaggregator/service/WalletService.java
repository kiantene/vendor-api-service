package com.nextgen.gameaggregator.service;

import com.couchbase.client.core.error.AmbiguousTimeoutException;
import com.couchbase.client.core.error.UnambiguousTimeoutException;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.common.FeatureToggle;
import com.nextgen.gameaggregator.core.engine.game.round.GameRoundService;
import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextHolder;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.Features;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetAction;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultAction;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackAction;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.service.business.maxpayout.AgentMaxPayoutService;
import com.nextgen.gameaggregator.service.data.VendorFeatureDataService;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryPublishContext;
import com.nextgen.gameaggregator.util.EnvUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;
    private final Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
    private final BetResultLogService betResultLogService;
    private final BetRefundLogService betRefundLogService;
    private final WalletBalanceAction walletBalanceAction;
    private final WalletBetAction walletBetAction;
    private final WalletBetResultAction walletBetResultAction;
    private final WalletRollbackAction walletRollbackAction;
    private final UnsettledBetService unsettledBetService;
    private final SettledBetService settledBetService;
    private final KafkaService kafkaService;
    private final CachingService cachingService;
    private final LoggingService loggingService;
    private final BetNotFoundLogService betNotFoundLogService;
    private final VendorService vendorCurrencyConversionService;
    private final BetIdempotentLogService betIdempotentLogService;
    private final AgentMaxPayoutService agentMaxPayoutService;
    private final BetHistoryProducer betHistoryProducer;
    private final GameRoundService gameRoundService;
    private final VendorFeatureDataService vendorFeatureService;
    @Value("${endround-process.retry-max-attempts:5}")
    private int retryMaxAttempts;
    @Value("${endround-process.retry-vendor-list:}") // example value in properties or yml file > 1,4,19
    private String retryVendorList;

    public BigDecimal getBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) throws InvalidOperatorResponseException, InvalidAgentApiCredentialException, VendorCurrencyNotSupportException {
        if (httpRequestLog != null) {
            httpRequestLog.setRequestType(WalletBalanceAction.class.getSimpleName());
            httpRequestLog.setOperatorUsername(gameSession.getAgentPlayerUsername());
            httpRequestLog.setVendorId(gameSession.getVendorId());
            httpRequestLog.setGameToken(gameSession.getToken());
            httpRequestLog.setBetStart(System.currentTimeMillis());
            httpRequestLog.setVendorUsername(gameSession.getVendorPlayerUsername());
            httpRequestLog.setVendorGameCode(gameSession.getVendorGameCode());

        }

        WalletBalanceVo balanceVo = null;

        try {
            balanceVo = walletBalanceAction.call(traceId, gameSession, httpRequestLog);
            // TODO: to handle balance returned with more than 4 decimals
            // TODO: implement error handling

        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
            throw new VendorCurrencyNotSupportException();
        } finally {
            if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());
        }

        return balanceVo.getData().getBalance();
    }

    /**
     * To process the unsettled bet by sending the bet data to Operator to validate the player has sufficient balance
     * to place the bet.
     * <p>
     * When the Operator has responded with sufficient balance, we will save a record of the bet
     * as Unsettled.
     *
     * @param traceId       A unique Id for this request
     * @param gameSession   GameSession object containing information of the vendor, game, player
     * @param betResultData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @param rawData       Raw data sent by vendor containing information of the bet
     * @return The player's current wallet balance after deducting the bet amount
     */
    public BetEvent processBet(String traceId, GameSession gameSession, BetResultData betResultData, String rawData, HttpRequestLog httpRequestLog) throws
            InsufficientBalanceException, CouchbaseDataIntegrityException, InvalidOperatorResponseException,
            InvalidAgentApiCredentialException, BetResultIdempotentViolationException, TransactionStillProcessingException, VendorCurrencyNotSupportException {

        //log.info("processBet (" + traceId + "): " + betResultData);
        if (httpRequestLog != null) {
            httpRequestLog.setRequestType(WalletBetAction.class.getSimpleName());
            httpRequestLog.setOperatorUsername(gameSession.getAgentPlayerUsername());
            httpRequestLog.setVendorId(gameSession.getVendorId());
            httpRequestLog.setVendorBetId(betResultData.getVendorBetId());
            httpRequestLog.setRoundId(betResultData.getRoundId());
            httpRequestLog.setGameToken(gameSession.getToken());
            httpRequestLog.setBetStart(System.currentTimeMillis());
            httpRequestLog.setVendorUsername(gameSession.getVendorPlayerUsername());
            httpRequestLog.setVendorGameCode(gameSession.getVendorGameCode());

        }

        loggingService.logStart();
        UnsettledBet unsettledBet = unsettledBetService.idempotentCheck(traceId, gameSession, betResultData, rawData, ResultType.BET);
        loggingService.logProcessTime("processBet ｜ unsettledBetService.idempotentCheck", traceId);

        BetEvent betEvent;
        try {
            // record operator processing time
            WalletBalanceVo balanceVo = walletBetAction.call(traceId, gameSession, unsettledBet, httpRequestLog);

            BigDecimal balance = balanceVo.getData().getBalance();
            unsettledBet.setOperatorStatus(this.operatorStatusSuccess);
            unsettledBet.setBalance(balance);

            unsettledBetService.save(unsettledBet);
            betEvent = new BetEvent(unsettledBet, balance);

            // will insert bet info record to this topic for MG
            if (unsettledBet.getVendorId().equals(17)) {
                kafkaService.produceBetTransactionLog(unsettledBet, betResultData, gameSession.getVendorPlayerUsername());
            }

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {

            // record status code from operator if they return an error
            Integer operatorStatus = invalidOperatorResponseException.getOperatorStatus();
            unsettledBet.setOperatorStatus(operatorStatus);

            if (operatorStatus.equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                loggingService.logStart();
                cachingService.updateUnsettledBetCaching(unsettledBet);
                unsettledBetService.deleteWithoutClearingCache(unsettledBet);
                loggingService.logProcessTime("processBet ｜ when invalidOperatorResponseException.SC_INSUFFICIENT_FUNDS, unsettledBetService.deleteWithoutClearingCache", traceId);
                throw new InsufficientBalanceException();

            } else {
                loggingService.logStart();
                unsettledBetService.save(unsettledBet);
                loggingService.logProcessTime("processBet ｜ when invalidOperatorResponseException, unsettledBetService.save", traceId);
                throw invalidOperatorResponseException;

            }

        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
            unsettledBet.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            unsettledBetService.save(unsettledBet);
            log.warn("walletBetAction.call.vendorCurrencyNotSupportException traceId [" + traceId + "]: externalTransactionId (" + unsettledBet.getExternalTransactionId() + ") vendorPlayerId (" + unsettledBet.getVendorPlayerId() + ")");
            throw new VendorCurrencyNotSupportException();

        } finally {
            if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());
        }

        return betEvent;
    }

    private UnsettledBet getUnsettledBetFromRound(List<UnsettledBet> betList, String roundId, BetResultData betResultData) throws BetNotFoundException {
        if (betList.isEmpty()) throw new BetNotFoundException("Cannot find unsettled bets with round Id: " + roundId);

        UnsettledBet unsettledBet;

        //default will be using first unsettledBet from the list
        if (betList.size() == 1) { // single bet
            unsettledBet = betList.get(0);
        } else {
            // this is to handle PP multihand blackjack
            unsettledBet = betList.get(betList.size() - 1);
        }

        if (betResultData.getVendorBetId() != null) {
            //will check across with the vendorBetId pass in
            Optional<UnsettledBet> matchingBet = betList.stream()
                    .filter(isUnsettledBetMatch -> betResultData.getVendorBetId().equals(isUnsettledBetMatch.getVendorBetId()))
                    .findFirst();

            //if found matched then unsettledBet reassign to the unsettledBet that is matched
            //else then will be using the default unsettledBet
            if (matchingBet.isPresent()) {
                unsettledBet = matchingBet.get();

            }
        }

        return unsettledBet;
    }

    private WalletBalanceVo doSettledBetResult(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, BaseVendorService vendorService, HttpRequestLog httpRequestLog, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate)
            throws BetNotFoundException, InvalidAgentApiCredentialException, InvalidOperatorResponseException,
            TransactionStillProcessingException, BetResultIdempotentViolationException, VendorCurrencyNotSupportException, InternalServerTimeoutRetryException, MergedBetDataIntegrityException {

        String rawData = httpRequestLog.getRequestBody();
        String roundId = betResultData.getRoundId();
        Integer agentId = gameSession.getAgentId();
        BetInformation walletBetResultData = null;
        UnsettledBet unsettledBet = null;
        WalletBalanceVo balanceVo = null;
        SettledBet settledBet = null;

        // check for idempotency in settled_bet
        loggingService.logStart();
        try {
            settledBet = settledBetService.idempotentCheck(traceId, gameSession, betResultData);
        } catch (AmbiguousTimeoutException | UnambiguousTimeoutException couchbaseTimeoutException) {
            throw new InternalServerTimeoutRetryException(couchbaseTimeoutException.getMessage());
        }

        loggingService.logProcessTime("doSettledBetResult ｜ settledBetService.idempotentCheck", traceId);

        boolean retry = false;

        if (settledBet != null) {
            // to exclude PP endRound due to endRound mapping vendorBetId to roundId
            // IMPORTANT: refer to PP betDto and PP endRoundDto on vendorBetId mapping
            switch (resultType) {
                case BET_WIN, BET_LOSE -> retry = true;
            }
        }

        SettledBet updateCachingSettledBet = settledBet;
        List<UnsettledBet> unsettledBetList = null;

        if (!retry) {

            if (resultType == ResultType.BET_WIN || resultType == ResultType.BET_LOSE) { // PGSoft
                unsettledBet = unsettledBetService.newUnsettledBet(gameSession, rawData, betResultData, traceId, resultType.code);
                settledBet = new SettledBet(unsettledBet, vendorService, traceId);
                walletBetResultData = settledBet;
                updateCachingSettledBet = settledBet;

            } else {
                loggingService.logStart();
                unsettledBetList = vendorService.getVendorClassFileUnsettledBetList();
                if (Objects.isNull(unsettledBetList) || unsettledBetList.isEmpty()) {
                    unsettledBet = unsettledBetService.getUnsettledBet(betResultData, roundId, gameSession, httpRequestLog);
                } else {
                    unsettledBet = this.getUnsettledBetFromRound(unsettledBetList, roundId, betResultData);
                }
                loggingService.logProcessTime("doSettledBetResult ｜ unsettledBetService.getByRoundId", traceId);

                switch (resultType) {
                    case LOSE, END -> { // PP
                        // handle if settle end/lose resultType having isFreeSpin = 1.
                        unsettledBet.setIsFreespin((betResultData.getIsFreespin() == 1) ? betResultData.getIsFreespin() : unsettledBet.getIsFreespin());

                        //if end got new effective turnover, will include accordingly.
                        if (betResultData.getEffectiveTurnover() != null && betResultData.getEffectiveTurnover().compareTo(BigDecimal.ZERO) != 0) {
                            unsettledBet.setEffectiveTurnover(betResultData.getEffectiveTurnover());
                        }

                        this.mergeResultIntoBetDataForEndCondition(unsettledBet, betResultData);
                        settledBet = new SettledBet(unsettledBet, vendorService, traceId);
                        walletBetResultData = settledBet;

                        updateCachingSettledBet = new SettledBet(unsettledBet, vendorService, traceId);
                        updateCachingSettledBet.setVendorBetId(betResultData.getVendorBetId());
                        updateCachingSettledBet.setRoundId(betResultData.getRoundId());

                    }
                    case WIN -> { // CQ9

                        String internalTransactionId = traceId;
                        if (settledBet != null) { // if not null, means vendor resends the settled bet
                            // internal transaction id to use the previous value for operator call
                            internalTransactionId = settledBet.getInternalTransactionId();
                        }

                        this.mergeResultIntoBetData(unsettledBet, betResultData, resultType, traceId);
                        settledBet = new SettledBet(unsettledBet, vendorService, traceId);

                        //do not send aggregated settledBet as betResultDataForOperator for settled and win scenario
                        walletBetResultData = new SettledBet(betResultData, internalTransactionId, unsettledBet.getVendorGameId(), unsettledBet.getVendorPlayerId(), gameSession);
                        walletBetResultData.setBetAmount(BigDecimal.ZERO);
                        walletBetResultData.setBetId(settledBet.getBetId());
                        walletBetResultData.setVendorBetTime(settledBet.getVendorBetTime());
                        walletBetResultData.setWinLoss(settledBet.getWinLoss());
                        walletBetResultData.setEffectiveTurnover(settledBet.getEffectiveTurnover());
                        walletBetResultData.setVendorId(gameSession.getVendorId());
                        walletBetResultData.setGameSessionToken(unsettledBet.getGameSessionToken());

                        //when update caching settle bet, it should be using walletBetResultData, instead of settledBet data, because settledBet data is get from unsettledBet
                        updateCachingSettledBet = new SettledBet(walletBetResultData);
                        //ga-2684, handle for hacksaw
                        updateCachingSettledBet.setBetAmount(settledBet.getBetAmount());
                        settledBet.setGameSessionToken(unsettledBet.getGameSessionToken());

                    }
                }
            }
        } else {
            // resend transaction to Operator due to operator error from previous request
            walletBetResultData = settledBet;

        }

        // send bet data to Operator
        try {
            this.processDefaultDataForSettledBet(walletBetResultData, settledBet);
            walletBetResultData.setBalance(settledBet.getBalance());

            BetInformation cappedBetResult = agentMaxPayoutService.applyPayoutCap(walletBetResultData);

            if (this.doCheckPPEndRoundForceProcessRetry(gameSession.getVendorId(), resultType, cappedBetResult.getWinAmount(), settledBet.getOperatorStatus())) {
                balanceVo = walletBetResultAction.generateOperatorBetResultInfoAndForceRetry(
                        traceId,
                        agentId,
                        gameSession,
                        cappedBetResult,
                        resultType,
                        httpRequestLog,
                        fromVendorConversionRate);
            } else {
                balanceVo = walletBetResultAction.call(
                        traceId,
                        agentId,
                        gameSession,
                        cappedBetResult,
                        resultType,
                        httpRequestLog,
                        fromVendorConversionRate,
                        toVendorConversionRate,
                        vendorService.operatorTimeoutTiming()
                );
            }

            loggingService.logStart();
            cachingService.storePlayerLatestBalanceToRedis(gameSession, balanceVo.getData().getBalance());
            loggingService.logProcessTime("doSettledBetResult ｜ cachingService.storePlayerLatestBalanceToRedis", traceId);

            // update operator status after receiving response from operator
            settledBet.setOperatorStatus(operatorStatusSuccess);
            settledBet.setBalance(balanceVo.getData().getBalance());

            loggingService.logStart();
            // settledBet is modified via applyPayoutCap through walletBetResultData
            settledBetService.save(settledBet, rawData);
            loggingService.logProcessTime("doSettledBetResult ｜ after walletBetResultAction.call, settledBetService.save", traceId);

            // remap settleBet info before insert into kafka if needed, default will be no changes
            settledBet = vendorService.updateSettleBetDataBeforeInsertToKafka(settledBet, httpRequestLog.getRequestBody());

            // send settled bet to kafka
            if (FeatureToggle.useRefactoredPublishBetHistory(settledBet.getVendorId())
                || vendorFeatureService.isVendorEnabled(Features.AGENT_MAX_PAYOUT, settledBet.getVendorId())) {
                /**
                 * Feature toggle to use refactored logic, will affect vendors who are enabled for max payout as well
                 */
                doPublishBetHistory(settledBet, vendorService, gameSession, fromVendorConversionRate, httpRequestLog);
            } else {
                /**
                 * @deprecated below logic to be removed, use doPublishBetHistory as the refactored version
                 */
                BetHistory betHistory = new BetHistory(settledBet);

                loggingService.logStart();
                if (!vendorService.getBetPreprocess().getIsPreProcessBet()) {
                    /**
                     * Exclude bet history produce logic if vendor is running on new framework
                     * @see {@link com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper}
                     */
                    if (!httpRequestLog.isBetHistoryProduceDisabled()) {
                        kafkaService.produceBetHistoryV3(betHistory, gameSession.getProductCode(), gameSession.getProductId(), gameSession.getProductGameId(),
                                gameSession.getAgentPlayerUsername(), gameSession.getVendorPlayerUsername(), fromVendorConversionRate);
                    }
                } else {
                    // process bet as preprocessing bet and send to kafka topic_bet_history_preprocessing topic
                    kafkaService.producePreprocessingBetHistory(betHistory, gameSession.getAgentPlayerUsername(), gameSession.getVendorPlayerUsername(), fromVendorConversionRate);
                }
                loggingService.logProcessTime("doSettledBetResult ｜ kafkaService.produceBetHistory", traceId);
            }

            // will insert bet info record to this topic for MG
            if (settledBet.getVendorId().equals(17)) {
                kafkaService.produceBetTransactionLog(settledBet, betResultData, gameSession.getVendorPlayerUsername());
            }

            // delete unsettle bet only for vendors that will insert unsettle bet
            if (resultType == ResultType.WIN || resultType == ResultType.LOSE || resultType == ResultType.END) {
                loggingService.logStart();
                unsettledBetService.delete(unsettledBet);
                loggingService.logProcessTime("doSettledBetResult ｜ unsettledBetService.delete", traceId);
            }

            loggingService.logStart();
            betIdempotentLogService.create(betResultData, settledBet.getBalance(), gameSession);
            loggingService.logProcessTime("doSettledBetResult ｜ betIdempotentLogService.create", traceId);

            if (resultType == ResultType.LOSE || resultType == ResultType.END || resultType == ResultType.WIN) {
                loggingService.logStart();
                updateCachingSettledBet = settledBetService.update(operatorStatusSuccess, balanceVo.getData().getBalance(), updateCachingSettledBet);
                loggingService.logProcessTime("doSettledBetResult ｜ settledBetService.update", traceId);
            }

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBet.setOperatorStatus(invalidOperatorResponseException.getOperatorStatus());

            // update operator status after receiving response from operator
            if (resultType == ResultType.LOSE || resultType == ResultType.END || resultType == ResultType.WIN) {
                loggingService.logStart();
                settledBetService.update(invalidOperatorResponseException.getOperatorStatus(), BigDecimal.ZERO, updateCachingSettledBet);
                loggingService.logProcessTime("doSettledBetResult ｜ when invalidOperatorResponseException, settledBetService.update", traceId);

            } else {
                settledBetService.save(settledBet, rawData);
                if (settledBet.getOperatorStatus() == ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code) {
                    if (resultType == ResultType.LOSE || resultType == ResultType.END || resultType == ResultType.WIN) {
                        unsettledBetService.deleteWithoutClearingCache(unsettledBet);
                    }

                }
            }
            throw invalidOperatorResponseException;

        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
            settledBet.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            settledBetService.save(settledBet, rawData);

            log.warn("walletBetResultAction.call.vendorCurrencyNotSupportException traceId [" + traceId + "]: externalTransactionId (" + settledBet.getExternalTransactionId() + ") vendorPlayerId (" + settledBet.getVendorPlayerId() + ")");
            throw new VendorCurrencyNotSupportException();
        } finally {
            httpRequestLog.setBetEnd(System.currentTimeMillis());
        }

        doEndRoundProcess(traceId, betResultData, settledBet, vendorService, gameSession);

        return balanceVo;
    }

    private void doPublishBetHistory(SettledBet settledBet,
                                     BaseVendorService vendorService,
                                     GameSession gameSession,
                                     BigDecimal fromVendorConversionRate,
                                     HttpRequestLog httpRequestLog) {

        if (httpRequestLog.isBetHistoryProduceDisabled()) return;

        boolean requirePreprocessing = vendorService.getBetPreprocess().getIsPreProcessBet();
        var mode = BetResultConfig.ProcessingMode.SINGLE;
        List<BetTransaction> txnList = null;

        if (BetResultContextHolder.isInitialized()) {
            mode = BetResultContextHolder.getConfig().getProcessingMode();
            txnList = BetResultContextHolder.getBetResultContext().getBetTransactions();
        }

        BetHistoryPublishContext publishContext = new BetHistoryPublishContext(
                gameSession.getProductCode(),
                gameSession.getProductId(),
                gameSession.getProductGameId(),
                gameSession.getAgentPlayerUsername(),
                gameSession.getVendorPlayerUsername(),
                fromVendorConversionRate,
                requirePreprocessing,
                txnList
        );

        betHistoryProducer.publish(settledBet, publishContext, mode);
    }

    private void doEndRoundProcess(String traceId, BetResultData betResultData, SettledBet settledBet, BaseVendorService vendorService, GameSession gameSession) {
        // overwrite by new integration framework: WalletBetResultServiceWrapper
        if (BetResultContextHolder.isInitialized()) {
            SettleType settleType = BetResultContextHolder.getConfig().getSettleType();
            if (settleType.equals(SettleType.BET)) return; // if settle by bet, don't run below
        }

        loggingService.logStart();
        /**
         * The logic below indicates settle by round (default behaviour from old integration framework using BaseVendorService)
         * In the new integration framework, default behaviour is settled by bet and BaseVendorService is no longer in used,
         * replaced by BetResultConfig which sets the settleType (refer to above logic)
         */
        if (!betResultData.getShouldSettleByBet()) {
            // Get the list of vendors from ENV for retry vendor
            List<Integer> vendorList = EnvUtils.getVendorListFromEnv(this.retryVendorList);

            // Check if the vendor is eligible to process the end round
            if (vendorList.contains(settledBet.getVendorId())) {
                loggingService.logDataFlowByVendor("Before executeRetryEndRound without taskScheduler", settledBet.getVendorId(), settledBet.getRoundId(), settledBet);
                gameRoundService.executeRetryEndRound(settledBet, vendorService, gameSession, traceId, this.retryMaxAttempts + 1);

            } else {
                loggingService.logDataFlowByVendor("Before notifyEndRoundAsync", settledBet.getVendorId(), settledBet.getRoundId(), settledBet);
                gameRoundService.notifyEndRoundAsync(settledBet, vendorService, gameSession, traceId);
            }
        }
        //else settle by bet, which no need to run endRoundAsync.
        loggingService.logProcessTime("doSettledBetResult ｜ walletService.notifyEndRoundAsync", traceId);
    }

    private void processDefaultDataForSettledBet(BetInformation betInformation, SettledBet settledBet) {

        Long vendorBetTime = (settledBet.getVendorBetTime() == null) ? System.currentTimeMillis() : settledBet.getVendorBetTime();
        Long vendorSettleTime = (settledBet.getVendorSettleTime() == null) ? System.currentTimeMillis() : settledBet.getVendorSettleTime();
        Long resultTime = vendorSettleTime;

        settledBet.setVendorBetTime(vendorBetTime);
        settledBet.setVendorSettleTime(vendorSettleTime);
        settledBet.setResultTime(resultTime);

        betInformation.setVendorBetTime(vendorBetTime);
        betInformation.setVendorSettleTime(vendorSettleTime);
        betInformation.setResultTime(resultTime);
    }

    private boolean doCheckPPEndRoundForceProcessRetry(Integer vendorId, ResultType resultType, BigDecimal winAmount, Integer previousOperatorStatus) {

        boolean shouldForceRetry = false;

        if (vendorId.equals(1)) {
            if (resultType.equals(ResultType.END)) {
                if (winAmount.compareTo(BigDecimal.ZERO) != 0) {
                    if (!previousOperatorStatus.equals(ResponseCodes.Status.SC_OK.code)) {
                        shouldForceRetry = true;
                    }
                }
            }
        }

        return shouldForceRetry;
    }

    private SettledBet doCheckBetExistsInSettledBet(Long vendorPlayerId, String externalTransactionId, String traceId, Long vendorSettledTime, BaseVendorService vendorService, GameSession gameSession, String roundId)
            throws TransactionStillProcessingException, BetResultIdempotentViolationException, RecordNotFoundException {

        SettledBet settledBet = null;

        try {

            if (vendorService.shouldDoRollbackByRound(gameSession)) {
                checkExistsByRoundFromBetRefundLog(vendorPlayerId, roundId);
            }

            loggingService.logStart();
            settledBet = settledBetService.getByVendorPlayerIdAndExternalTransactionIdWithRetry(vendorPlayerId, externalTransactionId);
            loggingService.logProcessTime("doCheckBetExistsInSettledBet ｜ settledBetService.getByVendorPlayerIdAndExternalTransactionId", traceId);

            if (settledBet == null) {
                throw new BetNotFoundException();
            }

            Integer operatorStatus = settledBet.getOperatorStatus();
            Long betTimingDifferenceInMillieSeconds = betIdempotentLogService.compareWithExistingTimingDifference(settledBet.getCreateTime());
            Integer resettleNum = Optional.ofNullable(settledBet.getResettleNum()).orElse(0);

            if (settledBet.getStatus().equals(BetStatus.SETTLED.code)) {
                if (operatorStatus.equals(operatorStatusSuccess)) {
                    // if SC_OK
                    if (vendorService.shouldRejectCancelRequest()) {
                        throw new BetResultIdempotentViolationException(settledBet);
                    }
                    // continue processRollback request if should not rejected
                    resettleNum += 1;
                } else if (operatorStatus.equals(operatorStatusProcessing)) {
                    // if SC_TRANSACTION_STILL_PROCESSING
                    throw new TransactionStillProcessingException();

                } else if (settledBet.getVendorId().equals(5) && operatorStatus.equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                    //only for fachai.
                    throw new RecordNotFoundException();
                }
                // else then other operator error, continue processRollback request
                settledBet.setInternalTransactionId(traceId);
            } else {
                // if SC_OK
                if (operatorStatus.equals(operatorStatusSuccess)) {
                    throw new BetResultIdempotentViolationException(settledBet);
                }
                // it would be cancel / refund
                if (betTimingDifferenceInMillieSeconds < betIdempotentLogService.getTimingDifferenceForStillProcessing()) {
                    // if less than 5 seconds
                    throw new TransactionStillProcessingException();
                }
                // continue processRollback request.
                resettleNum = settledBet.getResettleNum();
            }

            if (vendorSettledTime != null) {
                // will be priority of using rollbackData vendorSettleTime if available.
                settledBet.setVendorSettleTime(vendorSettledTime);
            }
            settledBet.setOperatorStatus(operatorStatusProcessing);
            settledBet.setResettleNum(resettleNum);
            settledBetService.save(settledBet, settledBet.getRawData());

        } catch (BetNotFoundException betNotFoundException) {
            //return settleBet = null;
        }

        return settledBet;

    }

    private void checkExistsByRoundFromBetRefundLog(Long vendorPlayerId, String roundId) throws BetResultIdempotentViolationException {
        RawBetRefundLog rawBetRefundLog = betRefundLogService.checkExistsByRoundId(vendorPlayerId, roundId);

        if (rawBetRefundLog != null) {
            throw new BetResultIdempotentViolationException(rawBetRefundLog);
        }
    }

    private WalletBalanceVo doUnsettledBetResult(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, BaseVendorService vendorService, HttpRequestLog httpRequestLog, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate)
            throws BetNotFoundException, InvalidAgentApiCredentialException, InvalidOperatorResponseException,
            TransactionStillProcessingException, BetResultIdempotentViolationException, VendorCurrencyNotSupportException,
            InsufficientBalanceException, InternalServerTimeoutRetryException {

        String rawData = httpRequestLog.getRequestBody();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        Integer vendorGameId = gameSession.getVendorGameId();
        String roundId = betResultData.getRoundId();
        Integer agentId = gameSession.getAgentId();
        UnsettledBet unsettledBet = new UnsettledBet(betResultData, vendorGameId, vendorPlayerId);
        WalletBalanceVo balanceVo = null;
        RawBetResultLog rawBetResultLog = null;

        BetInformation walletBetResultData = unsettledBet;
        walletBetResultData.setInternalTransactionId(traceId);

        switch (resultType) {
            case WIN, LOSE -> { // PP Win, data contains only result
                // Idempotency checks on bet_result_log
                loggingService.logStart();
                try {
                    rawBetResultLog = betResultLogService.idempotentCheck(traceId, gameSession, betResultData);
                } catch (AmbiguousTimeoutException | UnambiguousTimeoutException couchbaseTimeoutException) {
                    throw new InternalServerTimeoutRetryException(couchbaseTimeoutException.getMessage());
                }

                loggingService.logProcessTime("doUnsettledBetResult ｜ betResultLogService.idempotentCheck", traceId);

                loggingService.logStart();
                List<UnsettledBet> unsettledBetList = vendorService.getVendorClassFileUnsettledBetList();
                if (Objects.isNull(unsettledBetList) || unsettledBetList.isEmpty()) {
                    unsettledBet = unsettledBetService.getUnsettledBet(betResultData, roundId, gameSession, httpRequestLog);
                } else {
                    unsettledBet = this.getUnsettledBetFromRound(unsettledBetList, roundId, betResultData);
                }
                loggingService.logProcessTime("doUnsettledBetResult ｜ unsettledBetService.getByRoundId", traceId);

                // when betHistoryId is 0, it means this is a new request, so it is not a retry
                boolean retry = !rawBetResultLog.getBetHistoryId().equals("0");
                if (!retry) { // do not recalculate these values during retry, otherwise win amount will be double count
                    this.mergeResultIntoBetData(unsettledBet, betResultData, resultType, traceId);
                    unsettledBet.setWinLoss(vendorService.calculateWinLoss(unsettledBet));
                    unsettledBet.setEffectiveTurnover(vendorService.calculateEffectiveTurnover(unsettledBet));
                    rawBetResultLog.setBetHistoryId(unsettledBet.getBetId());
                }

                walletBetResultData.setBetId(unsettledBet.getBetId());
                walletBetResultData.setVendorBetTime(unsettledBet.getVendorBetTime());

                try {
                    // record operator processing time
                    walletBetResultData.setBalance(unsettledBet.getBalance());

                    BetInformation cappedBetResult = agentMaxPayoutService.applyPayoutCap(walletBetResultData);

                    balanceVo = walletBetResultAction.call(
                            traceId,
                            agentId,
                            gameSession,
                            cappedBetResult,
                            resultType,
                            httpRequestLog,
                            fromVendorConversionRate,
                            toVendorConversionRate,
                            vendorService.operatorTimeoutTiming()
                    );
                    BigDecimal balance = balanceVo.getData().getBalance();

                    rawBetResultLog.setOperatorStatus(this.operatorStatusSuccess);
                    rawBetResultLog.setBalance(balance);
                    betResultLogService.save(rawBetResultLog);

                    unsettledBet.setOperatorStatus(this.operatorStatusSuccess);
                    unsettledBet.setBalance(balance);

                    unsettledBetService.save(unsettledBet);

                    // will insert bet info record to this topic for MG
                    if (unsettledBet.getVendorId().equals(17)) {
                        kafkaService.produceBetTransactionLog(unsettledBet, betResultData, gameSession.getVendorPlayerUsername());
                    }

                } catch (InvalidOperatorResponseException invalidOperatorResponseException) {

                    // record status code from operator if they return an error
                    Integer operatorStatus = invalidOperatorResponseException.getOperatorStatus();
                    rawBetResultLog.setOperatorStatus(operatorStatus);
                    unsettledBet.setOperatorStatus(operatorStatus);

                    loggingService.logStart();

                    if (resultType.equals(ResultType.BET_WIN) || resultType.equals(ResultType.BET_LOSE)) {
                        if (operatorStatus.equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                            cachingService.updateUnsettledBetCaching(unsettledBet);
                            unsettledBetService.deleteWithoutClearingCache(unsettledBet);
                        }
                    } else {
                        betResultLogService.save(rawBetResultLog);
                        unsettledBetService.save(unsettledBet);
                    }

                    loggingService.logProcessTime("doUnsettledBetResult ｜ when invalidOperatorResponseException, unsettledBetService.save", traceId);
                    throw invalidOperatorResponseException;

                } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
                    rawBetResultLog.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
                    unsettledBet.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
                    betResultLogService.save(rawBetResultLog);
                    unsettledBetService.save(unsettledBet);
                    log.warn("walletBetResultAction.call.vendorCurrencyNotSupportException traceId [" + traceId + "]: externalTransactionId (" + unsettledBet.getExternalTransactionId() + ") vendorPlayerId (" + unsettledBet.getVendorPlayerId() + ")");

                    throw new VendorCurrencyNotSupportException();

                } finally {
                    httpRequestLog.setBetEnd(System.currentTimeMillis());
                }
            }
            case BET_WIN, BET_LOSE -> { // data contains bet and result
                // Idempotency checks on unsettled_bet
                loggingService.logStart();
                unsettledBet = unsettledBetService.idempotentCheck(traceId, gameSession, betResultData, rawData, resultType);
                loggingService.logProcessTime("doUnsettledBetResult ｜ unsettledBetService.idempotentCheck", traceId);
                walletBetResultData = unsettledBet;

                try {
                    balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, walletBetResultData, resultType, httpRequestLog, fromVendorConversionRate, toVendorConversionRate, vendorService.operatorTimeoutTiming());

                    unsettledBet.setOperatorStatus(this.operatorStatusSuccess);
                    unsettledBet.setBalance(balanceVo.getData().getBalance());
                    unsettledBetService.save(unsettledBet);

                    // will insert bet info record to this topic for MG
                    if (unsettledBet.getVendorId().equals(17)) {
                        kafkaService.produceBetTransactionLog(unsettledBet, betResultData, gameSession.getVendorPlayerUsername());
                    }

                } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
                    // record status code from operator if they return an error
                    Integer operatorStatus = invalidOperatorResponseException.getOperatorStatus();
                    unsettledBet.setOperatorStatus(operatorStatus);

                    loggingService.logStart();
                    unsettledBetService.save(unsettledBet);
                    loggingService.logProcessTime("doUnsettledBetResult ｜ when invalidOperatorResponseException, unsettledBetService.save", traceId);
                    throw invalidOperatorResponseException;

                } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
                    unsettledBet.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
                    unsettledBetService.save(unsettledBet);

                    log.warn("walletBetResultAction.call.vendorCurrencyNotSupportException traceId [" + traceId + "]: externalTransactionId (" + unsettledBet.getExternalTransactionId() + ") vendorPlayerId (" + unsettledBet.getVendorPlayerId() + ")");
                    throw new VendorCurrencyNotSupportException();

                } finally {
                    httpRequestLog.setBetEnd(System.currentTimeMillis());
                }
            }
        }

        return balanceVo;
    }

    /**
     * To process the result of a bet by sending the bet result data to Operator so that the Operator can update
     * the player's balance.
     *
     * @param traceId        A unique Id for this request
     * @param gameSession    GameSession object containing information of the vendor, game, player
     * @param betResultData  UnsettledResultSettledData object containing information of the bet result
     * @param httpRequestLog HttpRequest data object containing all information about the requests
     * @return ResultBetEvent An event object containing Bet and Bet Result information as well as the last balance
     * that can be used for further processing, if required
     * @throws BetNotFoundException If no bet record is found
     */
    public BigDecimal processBetResult(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, BaseVendorService vendorService, HttpRequestLog httpRequestLog)
            throws BetNotFoundException, InvalidOperatorResponseException,
            InvalidAgentApiCredentialException, MergedBetDataIntegrityException, InsufficientBalanceException,
            TransactionStillProcessingException, BetResultIdempotentViolationException, VendorCurrencyNotSupportException, InternalServerTimeoutRetryException {

        httpRequestLog.setRequestType(WalletBetResultAction.class.getSimpleName());
        httpRequestLog.setOperatorUsername(gameSession.getAgentPlayerUsername());
        httpRequestLog.setVendorId(gameSession.getVendorId());
        httpRequestLog.setVendorBetId(betResultData.getVendorBetId());
        httpRequestLog.setRoundId(betResultData.getRoundId());
        httpRequestLog.setGameToken(gameSession.getToken());
        httpRequestLog.setBetStart(System.currentTimeMillis());
        httpRequestLog.setVendorUsername(gameSession.getVendorPlayerUsername());
        httpRequestLog.setVendorGameCode(gameSession.getVendorGameCode());

        WalletBalanceVo balanceVo;
        boolean isSettled = betResultData.getBetStatus().isValueOf(BetStatus.SETTLED.code);

        VendorCurrency vendorCurrency = vendorCurrencyConversionService.getCurrencyConversionRate(gameSession, traceId);

        if (isSettled) {
            balanceVo = this.doSettledBetResult(traceId, gameSession, betResultData, resultType, vendorService, httpRequestLog, vendorCurrency.getFromVendorRate(), vendorCurrency.getToVendorRate());
        } else { // bets not settled yet
            balanceVo = this.doUnsettledBetResult(traceId, gameSession, betResultData, resultType, vendorService, httpRequestLog, vendorCurrency.getFromVendorRate(), vendorCurrency.getToVendorRate());
        }

        return balanceVo.getData().getBalance();
    }

    public BigDecimal processPromo(String traceId, GameSession gameSession, BetResultData betResultData, String rawData)
            throws InvalidAgentApiCredentialException, InvalidOperatorResponseException,
            TransactionStillProcessingException, BetResultIdempotentViolationException, VendorCurrencyNotSupportException {

        betResultLogService.idempotentCheck(traceId, gameSession, betResultData);
        BigDecimal balance = this.getBalance(traceId, gameSession, null);
        balance = balance.add(betResultData.getWinAmount());

        betResultLogService.create(traceId, betResultData.getVendorBetId(), betResultData, gameSession, balance, 1);

        return balance;
    }

    private void mergeResultIntoBetDataForEndCondition(BetInformation betData, BetResultData betResultData) {

        if (betResultData.getResultTime() != null) {
            betData.setResultTime(betResultData.getResultTime());
        }

        if (betResultData.getVendorSettleTime() != null) {
            betData.setVendorSettleTime(betResultData.getVendorSettleTime());
        }

    }

    private void mergeResultIntoBetData(BetInformation betData, BetResultData betResultData, ResultType resultType, String traceId) {

        BigDecimal winAmount = Optional.ofNullable(betData.getWinAmount()).orElse(BigDecimal.ZERO);
        BigDecimal winAmountLatest = Optional.ofNullable(betResultData.getWinAmount()).orElse(BigDecimal.ZERO);
        BigDecimal finalWinAmount = winAmount.add(winAmountLatest);
        betData.setWinAmount(finalWinAmount);

        BigDecimal jackpotAmount = Optional.ofNullable(betData.getJackpotAmount()).orElse(BigDecimal.ZERO);
        BigDecimal jackpotAmountLatest = Optional.ofNullable(betResultData.getJackpotAmount()).orElse(BigDecimal.ZERO);
        BigDecimal finalJackpotAmount = jackpotAmount.add(jackpotAmountLatest);
        betData.setJackpotAmount(finalJackpotAmount);

        //if betResultData have effectiveTurnover amount, then assign the amount into betData (unsettledBet)
        if (betResultData.getEffectiveTurnover() != null && betResultData.getEffectiveTurnover().compareTo(BigDecimal.ZERO) != 0) {
            betData.setEffectiveTurnover(betResultData.getEffectiveTurnover());
        }

        betData.setResultTime(betResultData.getResultTime());
        betData.setVendorSettleTime(betResultData.getVendorSettleTime());
        betData.setIsFreespin(Optional.ofNullable(betResultData.getIsFreespin()).orElse(0));

    }

    public WalletRequest processRollback(RollbackData rollbackData, GameSession gameSession, BaseVendorService vendorService, HttpRequestLog httpRequestLog)
            throws InvalidAgentApiCredentialException, RecordNotFoundException, VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException, BetRefundIdempotentViolationException,
            TransactionStillProcessingException, InvalidOperatorResponseException, BetNotFoundException, InvalidFormatException {

        this.processRollback(httpRequestLog.getId(), rollbackData, gameSession, vendorService, httpRequestLog);

        return httpRequestLog.getWalletRequest();
    }

    /**
     * To process the reversal of a bet by sending the rollback instruction to Operator so that the Operator can perform
     * a reversal and return the updated balance of the player.
     *
     * @param traceId      A unique Id for this request
     * @param rollbackData Vendor's bet transaction Id of a previous bet record
     * @param gameSession  gameSession object containing information of the vendor, game, player
     * @return BetRefundEvent An event object containing Bet and Refund information to be used for further processing, if required
     * @throws BetNotFoundException    If no bet record is found
     * @throws RecordNotFoundException Generic exception for orphan records
     */
    public BigDecimal processRollback(String traceId, RollbackData rollbackData, GameSession gameSession, BaseVendorService vendorService, HttpRequestLog httpRequestLog)
            throws RecordNotFoundException, InvalidAgentApiCredentialException,
            InvalidOperatorResponseException, BetRefundIdempotentViolationException, BetNotFoundException,
            BetResultIdempotentViolationException, TransactionStillProcessingException, VendorCurrencyNotSupportException, InvalidFormatException {

        httpRequestLog.setRequestType(WalletRollbackAction.class.getSimpleName());
        httpRequestLog.setOperatorUsername(gameSession.getAgentPlayerUsername());
        httpRequestLog.setVendorId(gameSession.getVendorId());
        httpRequestLog.setVendorBetId(rollbackData.getRollbackId());
        httpRequestLog.setRoundId(rollbackData.getRollbackId());
        httpRequestLog.setGameToken(gameSession.getToken());
        httpRequestLog.setBetStart(System.currentTimeMillis());
        httpRequestLog.setVendorUsername(gameSession.getVendorPlayerUsername());
        httpRequestLog.setVendorGameCode(gameSession.getVendorGameCode());

        Long vendorPlayerId = gameSession.getVendorPlayerId();
        BigDecimal balance = BigDecimal.ZERO;
        String externalTransactionId = rollbackData.getRollbackId();
        SettledBet settledBet = null;
        UnsettledBet unsettledBet = null;
        Long vendorSettledTime = rollbackData.getVendorSettledTime();
        BigDecimal fromVendorRate = BigDecimal.ONE;
        String roundId = rollbackData.getRoundId();

        WalletRequest walletRequest = httpRequestLog.getWalletRequest();

        try {
            settledBet = this.doCheckBetExistsInSettledBet(vendorPlayerId, externalTransactionId, traceId, vendorSettledTime, vendorService, gameSession, roundId);

            if (settledBet == null) {
                try {
                    loggingService.logStart();
                    unsettledBet = unsettledBetService.findBetsForRollback(vendorPlayerId, externalTransactionId);
                    loggingService.logProcessTime("unsettledBetService.findBetsForRollback", traceId);
                } catch (BetNotFoundException betNotFoundException) {
                    betNotFoundLogService.save(vendorPlayerId, rollbackData.getRollbackId(), BetStatus.REFUNDED, gameSession.getVendorId());
                    throw betNotFoundException;
                }

                settledBet = new SettledBet(unsettledBet, vendorService, traceId);
                settledBet.setOperatorStatus(operatorStatusProcessing);
                settledBet.setStatus(BetStatus.REFUNDED.code);

                if (vendorSettledTime != null) {
                    //will be priority of using rollbackData vendorSettleTime if available.
                    settledBet.setVendorSettleTime(vendorSettledTime);
                }

                if (settledBet.getVendorSettleTime() == null) {
                    //if still null for vendorSettleTime, will use current system time as vendorSettleTime
                    settledBet.setVendorSettleTime(System.currentTimeMillis());
                }

                settledBetService.save(settledBet, settledBet.getRawData());
                walletRequest.setBetAmount(settledBet.getBetAmount());
            }

            vendorSettledTime = settledBet.getVendorSettleTime();
            String betId = settledBet.getBetId();
            roundId = settledBet.getRoundId();
            Integer agentId = gameSession.getAgentId();
            String vendorBetId = settledBet.getVendorBetId();
            String internalTransactionId = settledBet.getInternalTransactionId();

            VendorCurrency vendorCurrency = vendorCurrencyConversionService.getCurrencyConversionRate(gameSession, traceId);
            fromVendorRate = vendorCurrency.getFromVendorRate();

            loggingService.logStart();
            WalletBalanceVo balanceVo = walletRollbackAction.call(traceId, agentId, gameSession, betId, roundId, vendorBetId, vendorSettledTime, internalTransactionId, httpRequestLog, vendorService.operatorTimeoutTiming());
            loggingService.logProcessTime("processRollback ｜ walletRollbackAction.call", traceId);

            //resettlement with below condition, then resettle_num need increase
            if (settledBet.getStatus().equals(BetStatus.SETTLED.code)) {
                settledBet.setStatus(BetStatus.CANCELLED.code);
                settledBet.setBetAmount(settledBet.getBetAmount().negate());
                settledBet.setWinAmount(settledBet.getWinAmount().negate());
                settledBet.setEffectiveTurnover(settledBet.getEffectiveTurnover().negate());
                settledBet.setWinLoss(settledBet.getWinLoss().negate());
                settledBet.setJackpotAmount(settledBet.getJackpotAmount().negate());
            }

            balance = (balanceVo.getData().getBalance().equals(BigDecimal.ZERO)) ? balance : balanceVo.getData().getBalance();
            settledBet.setOperatorStatus(operatorStatusSuccess);
            settledBet.setBalance(balance);
            walletRequest.setBalanceAfter(balance);
            walletRequest.setBalanceBefore(balance.subtract(settledBet.getWinLoss()));

            BetHistory betHistory = new BetHistory(settledBet);
            loggingService.logStart();
            // kafkaService.produceWarehouseBetHistory
            //         (betHistory, gameSession.getAgentPlayerUsername(), gameSession.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());
            kafkaService.produceBetHistoryV3(betHistory, gameSession.getProductCode(), gameSession.getProductId(), gameSession.getProductGameId(),
                    gameSession.getAgentPlayerUsername(), gameSession.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());

            loggingService.logProcessTime("processRollback ｜ kafkaService.produceBetHistory", traceId);

            loggingService.logStart();
            settledBetService.save(settledBet, " ");
            loggingService.logProcessTime("processRollback ｜ settledBetService.save", traceId);

            if (settledBet.getStatus().equals(BetStatus.REFUNDED.code)) {
                loggingService.logStart();
                if (unsettledBet != null) {
                    unsettledBetService.delete(unsettledBet);
                }
                loggingService.logProcessTime("processRollback ｜ unsettledBetService.delete", traceId);
            }

            // will insert bet info record to this topic for MG
            if (settledBet.getVendorId().equals(17)) {
                kafkaService.produceBetTransactionLog(settledBet, null, gameSession.getVendorPlayerUsername());
            }

            RawBetRefundLog rawBetRefundLog = betRefundLogService.newRawBetRefundLog(traceId, betId, rollbackData, roundId, gameSession, balance);
            loggingService.logStart();
            betRefundLogService.create(rawBetRefundLog);
            loggingService.logProcessTime("processRollback ｜ betRefundLogService.create", traceId);

            httpRequestLog.setBetEnd(System.currentTimeMillis());
            return balance;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            // update operator status after receiving response from operator
            settledBet.setOperatorStatus(invalidOperatorResponseException.getOperatorStatus());
            settledBetService.save(settledBet, "");
            httpRequestLog.setBetEnd(System.currentTimeMillis());
            throw invalidOperatorResponseException;

        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
            settledBet.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            settledBetService.save(settledBet, "");
            httpRequestLog.setBetEnd(System.currentTimeMillis());
            throw new VendorCurrencyNotSupportException();

        } catch (InvalidFormatException e) {
            settledBet.setOperatorStatus(ResponseCodes.Status.SC_INVALID_REQUEST.code);
            settledBetService.save(settledBet, "");

            BetHistory betHistory = new BetHistory(settledBet);
            kafkaService.produceOperatorRequestDlq(betHistory, fromVendorRate, gameSession.getVendorPlayerUsername());

            httpRequestLog.setBetEnd(System.currentTimeMillis());
            throw new InvalidFormatException(e.getAllValidationErrorMessages());
        }
        //need to catch all other exception?
    }
}
