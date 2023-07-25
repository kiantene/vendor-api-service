package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.UnsettledBetOperatorFailEvent;
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
    private BetHistoryService betHistoryService;
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

    private final Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;

    public BigDecimal getBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) throws InvalidOperatorResponseException, InvalidAgentApiCredentialException {
        if (httpRequestLog != null) {
            httpRequestLog.setOperatorUsername(gameSession.getAgentPlayerUsername());
            httpRequestLog.setVendorId(gameSession.getVendorId());
            httpRequestLog.setBetProcessStartTime(System.currentTimeMillis());
        }
        WalletBalanceVo balanceVo = walletBalanceAction.call(traceId, gameSession, httpRequestLog);
        // TODO: to handle balance returned with more than 4 decimals
        // TODO: implement error handling
        if (httpRequestLog != null) httpRequestLog.setBetProcessEndTime(System.currentTimeMillis());

        return balanceVo.getData().getBalance();
    }

    public BigDecimal getBalance(String traceId, GameSession gameSession) throws InvalidOperatorResponseException, InvalidAgentApiCredentialException {
        return this.getBalance(traceId, gameSession, null);
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
            InvalidAgentApiCredentialException, BetResultIdempotentViolationException, TransactionStillProcessingException {

        log.info("processBet (" + traceId + "): " + betResultData);
        if (httpRequestLog != null) {
            httpRequestLog.setOperatorUsername(gameSession.getAgentPlayerUsername());
            httpRequestLog.setVendorId(gameSession.getVendorId());
            httpRequestLog.setRoundId(betResultData.getRoundId());
            httpRequestLog.setBetProcessStartTime(System.currentTimeMillis());
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

            loggingService.logStart();
            unsettledBetService.save(unsettledBet);
            loggingService.logProcessTime("processBet ｜ when invalidOperatorResponseException, unsettledBetService.save", traceId);
            throw invalidOperatorResponseException;
        }

        if (httpRequestLog != null) httpRequestLog.setBetProcessEndTime(System.currentTimeMillis());

        return betEvent;
    }

    // This function will be deprecated, all vendor files must use processBet with httpRequestLog
    public BetEvent processBet(String traceId, GameSession gameSession, BetResultData betResultData, String rawData) throws
            InsufficientBalanceException, CouchbaseDataIntegrityException, InvalidOperatorResponseException,
            InvalidAgentApiCredentialException, BetResultIdempotentViolationException, TransactionStillProcessingException {

        return this.processBet(traceId, gameSession, betResultData, rawData, null);
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

    private WalletBalanceVo doSettledBetResult(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, BaseVendorService vendorService, HttpRequestLog httpRequestLog)
            throws BetNotFoundException, InvalidAgentApiCredentialException, InvalidOperatorResponseException,
            TransactionStillProcessingException, BetResultIdempotentViolationException {

        String rawData = httpRequestLog.getRequestBody();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        Integer vendorGameId = gameSession.getVendorGameId();
        String roundId = betResultData.getRoundId();
        Integer agentId = gameSession.getAgentId();
        BetInformation walletBetResultData = null;
        UnsettledBet unsettledBet = null;
        WalletBalanceVo balanceVo = null;
        SettledBet updateCachingSettledBet = new SettledBet();

        // check for idempotency in settled_bet
        loggingService.logStart();
        SettledBet settledBet = settledBetService.idempotentCheck(traceId, gameSession, betResultData);
        loggingService.logProcessTime("doSettledBetResult ｜ settledBetService.idempotentCheck", traceId);

        boolean retry = false;
        if (settledBet != null) {
            // to exclude PP endRound due to endRound mapping vendorBetId to roundId
            // IMPORTANT: refer to PP betDto and PP endRoundDto on vendorBetId mapping
            if (resultType != ResultType.LOSE && resultType != ResultType.END) {
                retry = true;
            }
            updateCachingSettledBet = settledBet;
        }

        loggingService.logStart();
        List<UnsettledBet> unsettledBetList = unsettledBetService.getByRoundId(roundId, vendorGameId, vendorPlayerId);
        loggingService.logProcessTime("doSettledBetResult ｜ unsettledBetService.getByRoundId", traceId);

        if (!retry) {
            switch (resultType) {
                case LOSE, END -> { // PP

                    unsettledBet = this.getUnsettledBetFromRound(unsettledBetList, roundId);

                    // handle if settle end/lose resultType having isFreeSpin = 1.
                    unsettledBet.setIsFreespin((betResultData.getIsFreespin() == 1) ? betResultData.getIsFreespin() : unsettledBet.getIsFreespin());
                    settledBet = new SettledBet(unsettledBet, vendorService, traceId);
                    walletBetResultData = settledBet;

                    updateCachingSettledBet = new SettledBet(unsettledBet, vendorService, traceId);
                    updateCachingSettledBet.setVendorBetId(betResultData.getVendorBetId());
                    updateCachingSettledBet.setRoundId(betResultData.getRoundId());

                }
                case WIN -> { // CQ9

                    unsettledBet = this.getUnsettledBetFromRound(unsettledBetList, roundId);
                    this.mergeResultIntoBetData(unsettledBet, betResultData, resultType, traceId);
                    settledBet = new SettledBet(unsettledBet, vendorService, traceId);

                    //do not send aggregated settledBet as betResultDataForOperator for settled and win scenario
                    walletBetResultData = new SettledBet(betResultData, traceId, unsettledBet.getVendorGameId(), unsettledBet.getVendorPlayerId());
                    walletBetResultData.setBetAmount(BigDecimal.ZERO);
                    walletBetResultData.setBetId(settledBet.getBetId());
                    walletBetResultData.setVendorBetTime(settledBet.getVendorBetTime());
                    walletBetResultData.setWinLoss(settledBet.getWinLoss());
                    walletBetResultData.setEffectiveTurnover(settledBet.getEffectiveTurnover());
                    updateCachingSettledBet = settledBet;

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
            balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, walletBetResultData, resultType, httpRequestLog);

            loggingService.logStart();
            cachingService.storePlayerLatestBalanceToRedis(gameSession, balanceVo.getData().getBalance());
            loggingService.logProcessTime("doSettledBetResult ｜ cachingService.storePlayerLatestBalanceToRedis", traceId);

            // update operator status after receiving response from operator
            settledBet.setOperatorStatus(operatorStatusSuccess);
            settledBet.setBalance(balanceVo.getData().getBalance());

            loggingService.logStart();
            settledBetService.save(settledBet, rawData);
            loggingService.logProcessTime("doSettledBetResult ｜ , after walletBetResultAction.call, settledBetService.save", traceId);

            // send settled bet to kafka
            BetHistory betHistory = new BetHistory(settledBet);

            loggingService.logStart();
            kafkaService.produceBetHistory(betHistory, settledBet);
            loggingService.logProcessTime("doSettledBetResult ｜ kafkaService.produceBetHistory", traceId);

            if (resultType == ResultType.LOSE || resultType == ResultType.END) {
                loggingService.logStart();
                updateCachingSettledBet = settledBetService.update(operatorStatusSuccess, balanceVo.getData().getBalance(), updateCachingSettledBet);
                loggingService.logProcessTime("doSettledBetResult ｜ settledBetService.update", traceId);
            }

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBet.setOperatorStatus(invalidOperatorResponseException.getOperatorStatus());
            // update operator status after receiving response from operator
            if (resultType == ResultType.LOSE || resultType == ResultType.END) {
                loggingService.logStart();
                settledBetService.update(invalidOperatorResponseException.getOperatorStatus(), BigDecimal.ZERO, updateCachingSettledBet);
                loggingService.logProcessTime("doSettledBetResult ｜ when invalidOperatorResponseException, settledBetService.update", traceId);
            } else {
                settledBetService.save(settledBet, rawData);
            }
            throw invalidOperatorResponseException;
        }

        loggingService.logStart();
        this.notifyEndRoundAsync(unsettledBetList, settledBet, vendorService, gameSession);
        loggingService.logProcessTime("doSettledBetResult ｜ walletService.notifyEndRoundAsync", traceId);

        return balanceVo;
    }

    private void notifyEndRoundAsync(List<UnsettledBet> unsettledBetList, SettledBet settledBet, BaseVendorService vendorService, GameSession gameSession) {
        if (unsettledBetList.isEmpty()) return;

        // multiple bets within same round
        for (UnsettledBet betRecord : unsettledBetList) {
            if (!settledBet.getId().equals(betRecord.getId())) { // exclude the current bet record
                String traceId = UUID.randomUUID().toString();
                SettledBet newSettledBet = new SettledBet(betRecord, vendorService, traceId);
                newSettledBet.setVendorSettleTime(settledBet.getVendorSettleTime());

                //AgentPlayerUsername, CurrencyCode and GameCode is used for walletBetResultAction.call when process end round result for operator
                EndRoundSettledBet endRoundSettledBet = new EndRoundSettledBet(newSettledBet, gameSession.getAgentPlayerUsername(),
                        gameSession.getCurrencyCode(), gameSession.getGameCode());
                endRoundSettledBet.setInternalTransactionId(traceId);

                kafkaService.produceEndRoundSettleBet(endRoundSettledBet);
            }
        }
    }

    private WalletBalanceVo doUnsettledBetResult(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, BaseVendorService vendorService, HttpRequestLog httpRequestLog)
            throws BetNotFoundException, InvalidAgentApiCredentialException, InvalidOperatorResponseException,
            TransactionStillProcessingException, BetResultIdempotentViolationException {

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
                    balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, walletBetResultData, resultType, httpRequestLog);
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
                    betResultLogService.save(rawBetResultLog);
                    unsettledBetService.save(unsettledBet);
                    loggingService.logProcessTime("doUnsettledBetResult ｜ when invalidOperatorResponseException, unsettledBetService.save", traceId);
                    throw invalidOperatorResponseException;
                }
            }
            case BET_WIN, BET_LOSE -> { // data contains bet and result
                // Idempotency checks on unsettled_bet
                loggingService.logStart();
                unsettledBet = unsettledBetService.idempotentCheck(traceId, gameSession, betResultData, rawData, resultType);
                loggingService.logProcessTime("doUnsettledBetResult ｜ unsettledBetService.idempotentCheck", traceId);
                walletBetResultData = unsettledBet;

                try {
                    balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, walletBetResultData, resultType, httpRequestLog);

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
            TransactionStillProcessingException, BetResultIdempotentViolationException {

        httpRequestLog.setOperatorUsername(gameSession.getAgentPlayerUsername());
        httpRequestLog.setVendorId(gameSession.getVendorId());
        httpRequestLog.setRoundId(betResultData.getRoundId());
        httpRequestLog.setBetProcessStartTime(System.currentTimeMillis());

        log.info("processBetResult:" + resultType + " (" + traceId + ") :" + betResultData);

        WalletBalanceVo balanceVo;
        boolean isSettled = betResultData.getBetStatus().isValueOf(BetStatus.SETTLED.code);

        if (isSettled) {
            balanceVo = this.doSettledBetResult(traceId, gameSession, betResultData, resultType, vendorService, httpRequestLog);
        } else { // bets not settled yet
            balanceVo = this.doUnsettledBetResult(traceId, gameSession, betResultData, resultType, vendorService, httpRequestLog);
        }
        httpRequestLog.setBetProcessEndTime(System.currentTimeMillis());

        return balanceVo.getData().getBalance();
    }

    public BigDecimal processPromo(String traceId, GameSession gameSession, BetResultData betResultData, String rawData)
            throws InvalidAgentApiCredentialException, InvalidOperatorResponseException,
            TransactionStillProcessingException, BetResultIdempotentViolationException {

        betResultLogService.idempotentCheck(traceId, gameSession, betResultData);
        BigDecimal balance = this.getBalance(traceId, gameSession);
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
    public BigDecimal processRollback(String traceId, RollbackData rollbackData, GameSession gameSession, BaseVendorService vendorService) throws
            RecordNotFoundException, InvalidAgentApiCredentialException,
            InvalidOperatorResponseException, BetRefundIdempotentViolationException, CouchbaseDataIntegrityException, BetNotFoundException {

        log.info("processRollback (" + traceId + "): " + rollbackData);

        Long vendorPlayerId = gameSession.getVendorPlayerId();
        BigDecimal balance = BigDecimal.ZERO;
        String externalTransactionId = rollbackData.getRollbackId();
        UnsettledBet unsettledBet = null;
        SettledBet settledBet = null;
        BetStatus betStatus = BetStatus.REFUNDED;

        // check idempotent
        this.betRefundLogService.idempotentCheck(gameSession.getVendorPlayerId(), gameSession.getVendorGameId(), externalTransactionId);

        try {
            unsettledBet = unsettledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);
        } catch (BetNotFoundException unsettledBetNotFoundException) {
            log.warn("processRollback -> BetNotFoundException in unsettled_bets: vendorPlayerId (" + vendorPlayerId + ") externalTransactionId (" + externalTransactionId + ")");
            try {
                settledBet = settledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);
                betStatus = BetStatus.CANCELLED;
            } catch (BetNotFoundException settledBetNotFoundException) {
                log.warn("processRollback -> BetNotFoundException in settled_bets: vendorPlayerId (" + vendorPlayerId + ") externalTransactionId (" + externalTransactionId + ")");
                throw settledBetNotFoundException;
            }
        }

        switch (betStatus) {
            case REFUNDED -> { //PP, CQ9
                String betId = unsettledBet.getBetId();
                String roundId = unsettledBet.getRoundId();
                Integer agentId = gameSession.getAgentId();
                String vendorBetId = unsettledBet.getVendorBetId();
                Long rollbackTimestamp = Optional.ofNullable(rollbackData.getVendorSettledTime()).orElse(unsettledBet.getVendorBetTime());

                WalletBalanceVo balanceVo = walletRollbackAction.call(traceId, agentId, gameSession, betId, roundId, vendorBetId, rollbackTimestamp);
                balance = balanceVo.getData().getBalance();

                SettledBet newSettledBet = new SettledBet(unsettledBet, vendorService, traceId);
                newSettledBet.setStatus(betStatus.code);
                newSettledBet.setVendorSettleTime(rollbackTimestamp);
                newSettledBet.setResultTime(rollbackTimestamp);
                newSettledBet.setResultType(ResultType.BET.code);
                newSettledBet.setEffectiveTurnover(BigDecimal.ZERO);
                newSettledBet.setWinLoss(BigDecimal.ZERO);

                settledBetService.save(newSettledBet, " ");
                BetHistory betHistory = new BetHistory(newSettledBet);
                log.info(new Gson().toJson(betHistory));
                kafkaService.produceBetHistory(betHistory, newSettledBet);

                RawBetRefundLog rawBetRefundLog = betRefundLogService.newRawBetRefundLog(traceId, betId, rollbackData, roundId, gameSession, balance);
                betRefundLogService.create(rawBetRefundLog);
                BetRefundLog betRefundLog = new BetRefundLog(rawBetRefundLog);
                log.info(new Gson().toJson(rawBetRefundLog));
                //TODO INSERT INTO KAFKA

                unsettledBetService.delete(unsettledBet);
            }
            case CANCELLED -> { //JILI, FACAI
                String betId = settledBet.getBetId();
                String roundId = settledBet.getRoundId();
                Integer agentId = gameSession.getAgentId();
                String vendorBetId = settledBet.getVendorBetId();
                Long rollbackTimestamp = Optional.ofNullable(rollbackData.getVendorSettledTime()).orElse(settledBet.getVendorSettleTime());

                //if data is exists on redis and status equal to cancel bet, then considered as duplicate rollback request.
                if (settledBet.getStatus() == BetStatus.CANCELLED.code) {
                    traceId = settledBet.getInternalTransactionId();
                    WalletBalanceVo balanceVo = walletRollbackAction.call(traceId, agentId, gameSession, betId, roundId, vendorBetId, rollbackTimestamp);
                    balance = balanceVo.getData().getBalance();

                } else {
                    WalletBalanceVo balanceVo = walletRollbackAction.call(traceId, agentId, gameSession, betId, roundId, vendorBetId, rollbackTimestamp);
                    balance = balanceVo.getData().getBalance();

                    settledBet.setInternalTransactionId(traceId);
                    settledBet.setStatus(betStatus.code);
                    settledBet.setVendorSettleTime(rollbackTimestamp);
                    settledBet.setResultTime(rollbackTimestamp);
                    settledBet.setResultType(ResultType.LOSE.code);
                    settledBet.setBetAmount(settledBet.getBetAmount().negate());
                    settledBet.setWinAmount(settledBet.getWinAmount().negate());
                    settledBet.setEffectiveTurnover(settledBet.getEffectiveTurnover().negate());
                    settledBet.setJackpotAmount(settledBet.getJackpotAmount().negate());
                    settledBet.setWinLoss(settledBet.getWinLoss().negate());
                    settledBet.setResettleNum(settledBet.getResettleNum() + 1);

                    settledBetService.save(settledBet, " ");
                    BetHistory betHistory = new BetHistory(settledBet);
                    log.info(new Gson().toJson(betHistory));
                    kafkaService.produceBetHistory(betHistory, settledBet);
                }
            }
            default -> log.warn("processRollback.exception -> bet status not handled");
        }

        return balance;
    }
}
