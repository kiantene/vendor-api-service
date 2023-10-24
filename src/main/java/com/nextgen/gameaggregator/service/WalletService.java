package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetStatus;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class WalletService {
    @Autowired
    private BetResultLogService betResultLogService;
    @Autowired
    private BetRefundLogService betRefundLogService;
    @Autowired
    private WalletBalanceAction walletBalanceAction;
    @Autowired
    private WalletBetAction walletBetAction;
    @Autowired
    private WalletBetResultAction walletBetResultAction;
    @Autowired
    private WalletRollbackAction walletRollbackAction;
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private CachingService cachingService;
    @Autowired
    private LoggingService loggingService;
    @Autowired
    private BetNotFoundLogService betNotFoundLogService;
    @Autowired
    private VendorService vendorCurrencyConversionService;
    @Autowired
    private BetIdempotentLogService betIdempotentLogService;

    private final Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;

    private final Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
    private final Integer internalServerError = ResponseCodes.Status.SC_UNKNOWN_ERROR.code;

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
            if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException){
            if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());
            throw new VendorCurrencyNotSupportException();
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

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {

            // record status code from operator if they return an error
            Integer operatorStatus = invalidOperatorResponseException.getOperatorStatus();
            unsettledBet.setOperatorStatus(operatorStatus);

            if (operatorStatus.equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                loggingService.logStart();
                unsettledBetService.deleteWithoutClearingCache(unsettledBet);
                loggingService.logProcessTime("processBet ｜ when invalidOperatorResponseException.SC_INSUFFICIENT_FUNDS, unsettledBetService.deleteWithoutClearingCache", traceId);
                throw new InsufficientBalanceException();

            } else {
                loggingService.logStart();
                unsettledBetService.save(unsettledBet);
                loggingService.logProcessTime("processBet ｜ when invalidOperatorResponseException, unsettledBetService.save", traceId);
                throw invalidOperatorResponseException;

            }

        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException){
            unsettledBet.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            unsettledBetService.save(unsettledBet);
            log.warn("walletBetAction.call.vendorCurrencyNotSupportException traceId [" + traceId + "]: externalTransactionId (" + unsettledBet.getExternalTransactionId() + ") vendorPlayerId (" + unsettledBet.getVendorPlayerId() + ")");
            throw new VendorCurrencyNotSupportException();

        }

        if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

        return betEvent;
    }

    private UnsettledBet getUnsettledBetFromRound(List<UnsettledBet> betList, String roundId) throws BetNotFoundException {
        if (betList.isEmpty()) throw new BetNotFoundException("Cannot find unsettled bets with round Id: " + roundId);

        UnsettledBet unsettledBet;
        if (betList.size() == 1) { // single bet
            unsettledBet = betList.get(0);
        } else {
            // this is to handle PP multihand blackjack
            unsettledBet = betList.get(betList.size() - 1);
        }
        return unsettledBet;
    }

    private WalletBalanceVo doSettledBetResult(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, BaseVendorService vendorService, HttpRequestLog httpRequestLog, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate)
            throws BetNotFoundException, InvalidAgentApiCredentialException, InvalidOperatorResponseException,
            TransactionStillProcessingException, BetResultIdempotentViolationException, VendorCurrencyNotSupportException {

        String rawData = httpRequestLog.getRequestBody();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        Integer vendorGameId = gameSession.getVendorGameId();
        String roundId = betResultData.getRoundId();
        Integer agentId = gameSession.getAgentId();
        BetInformation walletBetResultData = null;
        UnsettledBet unsettledBet = null;
        WalletBalanceVo balanceVo = null;

        // check for idempotency in settled_bet
        loggingService.logStart();
        SettledBet settledBet = settledBetService.idempotentCheck(traceId, gameSession, betResultData);
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
        loggingService.logStart();
        List<UnsettledBet> unsettledBetList = unsettledBetService.getByRoundId(roundId, vendorGameId, vendorPlayerId);
        loggingService.logProcessTime("doSettledBetResult ｜ unsettledBetService.getByRoundId", traceId);

        if (!retry) {
            switch (resultType) {
                case LOSE, END -> { // PP

                    unsettledBet = this.getUnsettledBetFromRound(unsettledBetList, roundId);

                    // handle if settle end/lose resultType having isFreeSpin = 1.
                    unsettledBet.setIsFreespin((betResultData.getIsFreespin() == 1) ? betResultData.getIsFreespin() : unsettledBet.getIsFreespin());

                    //if end got new effective turnover, will include accordingly.
                    if (betResultData.getEffectiveTurnover() != null && betResultData.getEffectiveTurnover().compareTo(BigDecimal.ZERO) != 0) {
                        unsettledBet.setEffectiveTurnover(betResultData.getEffectiveTurnover());
                    }

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

                    unsettledBet = this.getUnsettledBetFromRound(unsettledBetList, roundId);
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

                    //when update caching settle bet, it should be using walletBetResultData, instead of settledBet data, because settledBet data is get from unsettledBet
                    updateCachingSettledBet = new SettledBet(walletBetResultData);
                    //ga-2684, handle for hacksaw
                    updateCachingSettledBet.setBetAmount(settledBet.getBetAmount());

                }
                case BET_WIN, BET_LOSE -> { // PGSoft

                    unsettledBet = unsettledBetService.newUnsettledBet(gameSession, rawData, betResultData, traceId, resultType.code);
                    settledBet = new SettledBet(unsettledBet, vendorService, traceId);
                    walletBetResultData = settledBet;
                    updateCachingSettledBet = settledBet;

                }
            }
        } else {
            // resend transaction to Operator due to operator error from previous request
            walletBetResultData = settledBet;

        }

        // send bet data to Operator
        try {
            balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, walletBetResultData, resultType, httpRequestLog, fromVendorConversionRate, toVendorConversionRate);

            loggingService.logStart();
            cachingService.storePlayerLatestBalanceToRedis(gameSession, balanceVo.getData().getBalance());
            loggingService.logProcessTime("doSettledBetResult ｜ cachingService.storePlayerLatestBalanceToRedis", traceId);

            // update operator status after receiving response from operator
            settledBet.setOperatorStatus(operatorStatusSuccess);
            settledBet.setBalance(balanceVo.getData().getBalance());

            loggingService.logStart();
            settledBetService.save(settledBet, rawData);
            loggingService.logProcessTime("doSettledBetResult ｜ , after walletBetResultAction.call, settledBetService.save", traceId);

            // remap settleBet info before insert into kafka if needed, default will be no changes

            settledBet = vendorService.updateSettleBetDataBeforeInsertToKafka(settledBet, httpRequestLog.getRequestBody());

            // send settled bet to kafka
            BetHistory betHistory = new BetHistory(settledBet);

            loggingService.logStart();
            kafkaService.produceBetHistory(betHistory, settledBet, fromVendorConversionRate);
            loggingService.logProcessTime("doSettledBetResult ｜ kafkaService.produceBetHistory", traceId);

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

        }

        loggingService.logStart();
        this.notifyEndRoundAsync(unsettledBetList, settledBet, vendorService, gameSession, traceId);
        loggingService.logProcessTime("doSettledBetResult ｜ walletService.notifyEndRoundAsync", traceId);

        return balanceVo;
    }

    private SettledBet doCheckBetExistsInSettledBet(Long vendorPlayerId, String externalTransactionId, String traceId, Long vendorSettledTime, BaseVendorService vendorService)
            throws TransactionStillProcessingException, BetResultIdempotentViolationException {

        SettledBet settledBet = null;

        try {
            loggingService.logStart();
            settledBet = settledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);
            loggingService.logProcessTime("doCheckBetExistsInSettledBet ｜ settledBetService.getByVendorPlayerIdAndExternalTransactionId", traceId);

            Integer operatorStatus = settledBet.getOperatorStatus();
            // throw idempotent exception if status is processing or success
            if (operatorStatus.equals(operatorStatusProcessing)) {
                log.warn("getByVendorPlayerIdAndExternalTransactionId.processing [" + traceId + "]: externalTransactionId (" + settledBet.getExternalTransactionId() + ") vendorPlayerId (" + settledBet.getVendorPlayerId() + ")");
                throw new TransactionStillProcessingException();

            } else if (operatorStatus.equals(operatorStatusSuccess)) {
                if (vendorService.shouldRejectCancelRequest()) {
                    log.warn("getByVendorPlayerIdAndExternalTransactionId.success [" + traceId + "]: externalTransactionId (" + settledBet.getExternalTransactionId() + ") vendorPlayerId (" + settledBet.getVendorPlayerId() + ")");
                    throw new BetResultIdempotentViolationException(settledBet);

                } else {
                    if (!settledBet.getStatus().equals(BetStatus.SETTLED.code)) {
                        log.warn("getByVendorPlayerIdAndExternalTransactionId.success [" + traceId + "]: externalTransactionId (" + settledBet.getExternalTransactionId() + ") vendorPlayerId (" + settledBet.getVendorPlayerId() + ")");
                        throw new BetResultIdempotentViolationException(settledBet);

                    } else {
                        // proceed with entire cancel flow request.
                        Integer resettleNum = 0;

                        if (settledBet.getResettleNum() != null) {
                            resettleNum = settledBet.getResettleNum();
                        }

                        settledBet.setResettleNum(resettleNum + 1);

                    }
                }

            } else {
                // operatorStatus.equals(internalServerError)

            }

            // if status is settled, reset internalTransactionId and send cancel request to operator
            if (settledBet.getStatus().equals(BetStatus.SETTLED.code)) {
                settledBet.setStatus(BetStatus.CANCELLED.code);
                settledBet.setInternalTransactionId(traceId);
            }
            // else the betStatus is either refund or cancel (not settled), then will need to send with same txId to operator to cancel this bet

            if (vendorSettledTime != null) {
                // will be priority of using rollbackData vendorSettleTime if available.
                settledBet.setVendorSettleTime(vendorSettledTime);
            }
            settledBet.setOperatorStatus(operatorStatusProcessing);
            settledBetService.save(settledBet, settledBet.getRawData());

        } catch (BetNotFoundException betNotFoundException) {
            //return settleBet = null;
        }

        return settledBet;

    }

    private void notifyEndRoundAsync(List<UnsettledBet> unsettledBetList, SettledBet settledBet, BaseVendorService vendorService, GameSession gameSession, String traceId) {
        if (unsettledBetList.isEmpty()) return;

        // multiple bets within same round
        for (UnsettledBet betRecord : unsettledBetList) {
            if (!settledBet.getId().equals(betRecord.getId())) { // exclude the current bet record
                traceId = UUID.randomUUID().toString();

                //if unsettledBet data do have settledTime, then do not update by latest settledTime (PGSOFT CHANGES)
                if(betRecord.getVendorSettleTime() == null){
                    betRecord.setVendorSettleTime(settledBet.getVendorSettleTime());
                }

                SettledBet newSettledBet = new SettledBet(betRecord, vendorService, traceId);

                //AgentPlayerUsername, CurrencyCode and GameCode is used for walletBetResultAction.call when process end round result for operator
                EndRoundSettledBet endRoundSettledBet = new EndRoundSettledBet(newSettledBet, gameSession.getAgentPlayerUsername(),
                        gameSession.getCurrencyCode(), gameSession.getGameCode());
                endRoundSettledBet.setInternalTransactionId(traceId);

                kafkaService.produceEndRoundSettleBet(endRoundSettledBet);

            } else {
                loggingService.logStart();
                unsettledBetService.delete(betRecord);
                loggingService.logProcessTime("donNotifyEndRoundAsync ｜ unsettledBetService.delete", traceId);
                
            }
        }
    }

    private WalletBalanceVo doUnsettledBetResult(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, BaseVendorService vendorService, HttpRequestLog httpRequestLog, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate)
            throws BetNotFoundException, InvalidAgentApiCredentialException, InvalidOperatorResponseException,
            TransactionStillProcessingException, BetResultIdempotentViolationException, VendorCurrencyNotSupportException {

        String rawData = httpRequestLog.getRequestBody();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        Integer vendorGameId = gameSession.getVendorGameId();
        String roundId = betResultData.getRoundId();
        Integer agentId = gameSession.getAgentId();
        UnsettledBet unsettledBet = new UnsettledBet(betResultData, vendorGameId, vendorPlayerId);
        WalletBalanceVo balanceVo = null;

        BetInformation walletBetResultData = unsettledBet;
        walletBetResultData.setInternalTransactionId(traceId);

        switch (resultType) {
            case WIN, LOSE -> { // PP Win, data contains only result
                // Idempotency checks on bet_result_log
                loggingService.logStart();
                RawBetResultLog rawBetResultLog = betResultLogService.idempotentCheck(traceId, gameSession, betResultData);
                loggingService.logProcessTime("doUnsettledBetResult ｜ betResultLogService.idempotentCheck", traceId);

                loggingService.logStart();
                List<UnsettledBet> unsettledBetList = unsettledBetService.getByRoundId(roundId, vendorGameId, vendorPlayerId);
                loggingService.logProcessTime("doUnsettledBetResult ｜ unsettledBetService.getByRoundId", traceId);

                unsettledBet = this.getUnsettledBetFromRound(unsettledBetList, roundId);

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
                    balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, walletBetResultData, resultType, httpRequestLog, fromVendorConversionRate, toVendorConversionRate);
                    BigDecimal balance = balanceVo.getData().getBalance();

                    rawBetResultLog.setOperatorStatus(this.operatorStatusSuccess);
                    rawBetResultLog.setBalance(balance);
                    betResultLogService.save(rawBetResultLog);

                    unsettledBet.setOperatorStatus(this.operatorStatusSuccess);
                    unsettledBet.setBalance(balance);
                    unsettledBetService.save(unsettledBet);

                } catch (InvalidOperatorResponseException invalidOperatorResponseException) {

                    // record status code from operator if they return an error
                    Integer operatorStatus = invalidOperatorResponseException.getOperatorStatus();
                    rawBetResultLog.setOperatorStatus(operatorStatus);
                    unsettledBet.setOperatorStatus(operatorStatus);

                    loggingService.logStart();

                    if (operatorStatus == ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code) {
                        unsettledBetService.deleteWithoutClearingCache(unsettledBet);

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

                }
            }
            case BET_WIN, BET_LOSE -> { // data contains bet and result
                // Idempotency checks on unsettled_bet
                loggingService.logStart();
                unsettledBet = unsettledBetService.idempotentCheck(traceId, gameSession, betResultData, rawData, resultType);
                loggingService.logProcessTime("doUnsettledBetResult ｜ unsettledBetService.idempotentCheck", traceId);
                walletBetResultData = unsettledBet;

                try {
                    balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, walletBetResultData, resultType, httpRequestLog, fromVendorConversionRate, toVendorConversionRate);

                    unsettledBet.setOperatorStatus(this.operatorStatusSuccess);
                    unsettledBet.setBalance(balanceVo.getData().getBalance());
                    unsettledBetService.save(unsettledBet);

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
            TransactionStillProcessingException, BetResultIdempotentViolationException, VendorCurrencyNotSupportException {

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
        httpRequestLog.setBetEnd(System.currentTimeMillis());

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
            BetResultIdempotentViolationException, TransactionStillProcessingException, VendorCurrencyNotSupportException {

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

        try {
            settledBet = this.doCheckBetExistsInSettledBet(vendorPlayerId, externalTransactionId, traceId, vendorSettledTime, vendorService);

            if (settledBet == null) {
                try {
                    loggingService.logStart();
                    unsettledBet = unsettledBetService.findBetsForRollback(vendorPlayerId, externalTransactionId);
                    loggingService.logProcessTime("unsettledBetService.findBetsForRollback", traceId);
                } catch (BetNotFoundException betNotFoundException) {
                    betNotFoundLogService.save(vendorPlayerId, rollbackData.getRollbackId(), BetStatus.REFUNDED);
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
            }

            vendorSettledTime = settledBet.getVendorSettleTime();
            String betId = settledBet.getBetId();
            String roundId = settledBet.getRoundId();
            Integer agentId = gameSession.getAgentId();
            String vendorBetId = settledBet.getVendorBetId();
            String internalTransactionId = settledBet.getInternalTransactionId();

            VendorCurrency vendorCurrency = vendorCurrencyConversionService.getCurrencyConversionRate(gameSession, traceId);

            loggingService.logStart();
            WalletBalanceVo balanceVo = walletRollbackAction.call(traceId, agentId, gameSession, betId, roundId, vendorBetId, vendorSettledTime, internalTransactionId, httpRequestLog);
            loggingService.logProcessTime("processRollback ｜ walletRollbackAction.call", traceId);

            balance = balanceVo.getData().getBalance();
            settledBet.setOperatorStatus(operatorStatusSuccess);
            settledBet.setBalance(balance);

            loggingService.logStart();
            settledBetService.save(settledBet, " ");
            loggingService.logProcessTime("processRollback ｜ settledBetService.save", traceId);

            BetHistory betHistory = new BetHistory(settledBet);
            loggingService.logStart();
            kafkaService.produceBetHistory(betHistory, settledBet, vendorCurrency.getFromVendorRate());
            loggingService.logProcessTime("processRollback ｜ kafkaService.produceBetHistory", traceId);

            if (settledBet.getStatus().equals(BetStatus.REFUNDED.code)) {
                //only refund request need to insert into betRefundLog and delete unsettledBet
                RawBetRefundLog rawBetRefundLog = betRefundLogService.newRawBetRefundLog(traceId, betId, rollbackData, roundId, gameSession, balance);
                loggingService.logStart();
                betRefundLogService.create(rawBetRefundLog);
                loggingService.logProcessTime("processRollback ｜ betRefundLogService.create", traceId);

                BetRefundLog betRefundLog = new BetRefundLog(rawBetRefundLog);

                loggingService.logStart();
                unsettledBetService.delete(unsettledBet);
                loggingService.logProcessTime("processRollback ｜ unsettledBetService.delete", traceId);
            }

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

        }
        //need to catch all other exception?
    }
}
