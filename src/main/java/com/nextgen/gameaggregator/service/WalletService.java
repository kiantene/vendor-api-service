package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.ResultBetOperatorFailEvent;
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

    public BigDecimal getBalance(String traceId, GameSession gameSession) throws InvalidOperatorResponseException, InvalidAgentApiCredentialException {
        WalletBalanceVo balanceVo = walletBalanceAction.call(traceId, gameSession);
        // TODO: to handle balance returned with more than 4 decimals
        // TODO: implement error handling
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
    public BetEvent processBet(String traceId, GameSession gameSession, BetResultData betResultData, String rawData) throws
            InsufficientBalanceException, CouchbaseDataIntegrityException, InvalidOperatorResponseException,
            InvalidAgentApiCredentialException {

        log.info("processBet (" + traceId + "): " + betResultData);

        Integer agentId = gameSession.getAgentId();
        UnsettledBetOperatorFailEvent unsettledBetOperatorFailEvent = null;
        UnsettledBet unsettledBet = null;
        String vendorBetId = betResultData.getVendorBetId();
        String roundId = betResultData.getRoundId();
        Integer vendorGameId = gameSession.getVendorGameId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        boolean isBetExists = true;
        BigDecimal balance;
        BetEvent betEvent = new BetEvent(null, BigDecimal.ZERO);

        try {
            // checking if unsettled_bets table contains duplicate
            unsettledBet = unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);

            // Existing bet transaction found (idempotent), do not process again but get the latest balance from operator
            // TODO: add try-catch in case operator fails
            balance = this.getBalance(traceId, gameSession);
            betEvent = new BetEvent(unsettledBet, balance);

        } catch (BetNotFoundException betNotFoundException) { // no bets found, can proceed to process this bet txn
            isBetExists = false;
        }

        // no record in database, will proceed to send as bet request and insert as unsettled_bet data to couchbase
        if (!isBetExists) {
            try {
                // Send bet transaction to Operator to update player wallet
                WalletBalanceVo balanceVo = walletBetAction.call(traceId, agentId, gameSession, betResultData);
                balance = balanceVo.getData().getBalance();

                // Generate rawUnsettledBet and insert into couchbase unsettled_bets table
                unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, traceId, ResultType.BET.code);
                betHistoryService.createUnsettledBet(unsettledBet);
                betEvent = new BetEvent(unsettledBet, balance);

            } catch (InsufficientBalanceException insufficientBalanceException) {
                unsettledBetOperatorFailEvent = new UnsettledBetOperatorFailEvent(unsettledBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
                throw insufficientBalanceException;

            } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
                unsettledBetOperatorFailEvent = new UnsettledBetOperatorFailEvent(unsettledBet, invalidOperatorResponseException.getOperatorStatus());
                throw invalidOperatorResponseException;

            } finally {
                boolean isOperatorFailed = unsettledBetOperatorFailEvent != null;
                if (isOperatorFailed) {
                    // TODO: if operator failed, we just resend and does not need to update any status on unsettled bet, so eventing is not needed anymore
                    //EventDispatcherSystem.emitAsync(unsettledBetOperatorFailEvent);
                }
            }
        }

        return betEvent;
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
     * @throws BetNotFoundException            If no bet record is found
     * @throws CouchbaseDataIntegrityException If anything wrong data inser into couchbase Id is found
     */
    public BigDecimal processBetResult(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, BaseVendorService vendorService, HttpRequestLog httpRequestLog)
            throws BetNotFoundException, InvalidOperatorResponseException, CouchbaseDataIntegrityException,
            InvalidAgentApiCredentialException, MergedBetDataIntegrityException, InsufficientBalanceException,
            BetResultIdempotentViolationException {

        httpRequestLog.setBetProcessStartTime(System.currentTimeMillis());
        log.info("processBetResult:" + resultType + " (" + traceId + ") :" + betResultData);

        String rawData = httpRequestLog.getRequestBody();
        Integer agentId = gameSession.getAgentId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String roundId = betResultData.getRoundId();
        String vendorBetId = betResultData.getVendorBetId();
        Integer vendorGameId = gameSession.getVendorGameId();
        Integer vendorId = gameSession.getVendorId();
        ResultBetOperatorFailEvent resultBetOperatorFailEvent = null;
        Integer isVendorEqualsToPGSOFT = 2;

        boolean isSettled = betResultData.getBetStatus().isValueOf(BetStatus.SETTLED.code);

        try {
            // 1. Retrieve unsettled bet transaction to prepare for merging with bet result
            UnsettledBet unsettledBet = null;
            SettledBet settledBet = null;
            BigDecimal winLoss, effectiveTurnover;
            WalletBalanceVo balanceVo;
            BetInformation betResultDataForOperator = null;
            boolean isBetExistsForUnsettledBet = false;
            boolean isBetExistsForSettledBet = false;
            List<UnsettledBet> unsettledBetList = null;
            String traceIdPGSOFT = traceId;

            if (isSettled) {
                unsettledBetList = unsettledBetService.getByRoundId(roundId, vendorGameId, vendorPlayerId);

                try {
                    settledBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(vendorBetId, roundId, vendorId, vendorPlayerId);
                    isBetExistsForSettledBet = true;
                } catch (BetNotFoundException betNotFoundException) {
                    isBetExistsForSettledBet = false;
                }

                if (isBetExistsForSettledBet) {
                    if(gameSession.getVendorId() == isVendorEqualsToPGSOFT) {
                        traceIdPGSOFT = settledBet.getInternalTransactionId();
                    }
                    else {
                        httpRequestLog.setOperatorProcessStartTime(System.currentTimeMillis());
                        balanceVo = walletBalanceAction.call(traceId, gameSession);
                        httpRequestLog.setOperatorProcessEndTime(System.currentTimeMillis());

                        return balanceVo.getData().getBalance();
                    }
                }

                switch (resultType) {
                    case LOSE, END -> { // PP END
                        if (unsettledBetList.isEmpty()) {
                            throw new BetNotFoundException("resultType: " + resultType + " Cannot find round Id: " + roundId);
                        }

                        boolean isMultipleBetsInSameRound = unsettledBetList.size() > 1;
                        if (!isMultipleBetsInSameRound) { // single bet
                            unsettledBet = unsettledBetList.get(0);
                        } else { // multiple bets
                            unsettledBet = unsettledBetList.get(unsettledBetList.size() - 1);
                        }

                        //handle if settle end/lose resultType having isFreeSpin = 1.
                        unsettledBet.setIsFreespin((betResultData.getIsFreespin() == 1) ? betResultData.getIsFreespin() : unsettledBet.getIsFreespin());

                        settledBet = new SettledBet(unsettledBet, vendorService);
                        settledBet.setInternalTransactionId(traceId);

                        // handle PP END resultType but should be LOSE while winAmount less than equal to zero
//                        boolean isWinAmountMoreThanZero = settledBet.getWinAmount().compareTo(BigDecimal.ZERO) > 0;
//                        boolean isJackpotAmountMoreThanZero = settledBet.getJackpotAmount().compareTo(BigDecimal.ZERO) > 0;
//                        resultType = (isWinAmountMoreThanZero || isJackpotAmountMoreThanZero) ? ResultType.END : ResultType.LOSE;
                    }
                    case WIN -> { // CQ9 Win
                        unsettledBet = unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
                        this.mergeResultIntoBetData(unsettledBet, betResultData, resultType, traceId);
                        settledBet = new SettledBet(unsettledBet, vendorService);
                        settledBet.setInternalTransactionId(traceId);

                        //do not send aggregated settledBet as betResultDataForOperator for settled and win scenario
                        betResultDataForOperator = new SettledBet(betResultData);
                        betResultDataForOperator.setBetAmount(BigDecimal.ZERO);
                        betResultDataForOperator.setInternalTransactionId(traceId);
                        betResultDataForOperator.setBetId(settledBet.getBetId());
                        betResultDataForOperator.setVendorBetTime(settledBet.getVendorBetTime());
                        betResultDataForOperator.setWinLoss(settledBet.getWinLoss());
                        betResultDataForOperator.setEffectiveTurnover(settledBet.getEffectiveTurnover());
                    } // PGS
                    // PGS
                    case BET_WIN, BET_LOSE -> { // PGS
                        String betAndResultTraceId = (gameSession.getVendorId() == isVendorEqualsToPGSOFT)?traceIdPGSOFT:traceId;
                        unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, betAndResultTraceId, resultType.code);
                        settledBet = new SettledBet(unsettledBet, vendorService);
                    }
                    default -> log.warn("ProcessBetResult.exception -> result not handled");
                }

                if (settledBet != null) {
                    if (betResultData.getVendorSettleTime() != null) {
                        settledBet.setVendorSettleTime(betResultData.getVendorSettleTime());
                    } else {
                        settledBet.setVendorSettleTime(System.currentTimeMillis());
                    }
                }

                //if settled bet with resultType = WIN, then will prepare betResultDataForOperator in the switch case.
                if (resultType != ResultType.WIN) {
                    betResultDataForOperator = settledBet;
                }

            } else { // bets not settled yet
                betResultDataForOperator = new UnsettledBet(betResultData);
                betResultDataForOperator.setInternalTransactionId(traceId);

                // Idempotent checks
                this.idempotentCheckForBetResult(gameSession, betResultData);

                // create bet result log first with betId = 0
                betResultLogService.create(traceId, "0", betResultData, gameSession, BigDecimal.ZERO);

                switch (resultType) {
                    case WIN, LOSE -> { // PP Win
                        // check if bet record exists
                        unsettledBetList = unsettledBetService.getByRoundId(roundId, vendorGameId, vendorPlayerId);

                        if (unsettledBetList.isEmpty()) {
                            throw new BetNotFoundException("resultType: " + resultType + " Cannot find round Id: " + roundId);
                        }

                        if (unsettledBetList.size() == 1) { // only single bet
                            unsettledBet = unsettledBetList.get(0);
                        } else { // multiple bets
                            // if multiple bets, the result will be updated on the last bet
                            unsettledBet = unsettledBetList.get(unsettledBetList.size() - 1);
                        }

                        this.mergeResultIntoBetData(unsettledBet, betResultData, resultType, traceId);
                        winLoss = vendorService.calculateWinLoss(unsettledBet);
                        effectiveTurnover = vendorService.calculateEffectiveTurnover(unsettledBet);
                        unsettledBet.setWinLoss(winLoss);
                        unsettledBet.setEffectiveTurnover(effectiveTurnover);

                        betResultDataForOperator.setBetId(unsettledBet.getBetId());
                        betResultDataForOperator.setVendorBetTime(unsettledBet.getVendorBetTime());
                    }
                    case BET_WIN, BET_LOSE -> {
                        try {
                            unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
                            isBetExistsForUnsettledBet = true;
                        } catch (BetNotFoundException betNotFoundException) {
                            unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, traceId, resultType.code);
                            betResultDataForOperator = unsettledBet;
                        }
                    }

                    default -> log.warn("ProcessBetResult.exception -> result not handled");
                }

                // create unsettledBet first with betId = 0
                unsettledBet.setOperatorStatus(0);
                unsettledBetService.update(unsettledBet);

                // insert into unsettled_bet_result
//                unsettledBetService.update(unsettledBet);
                // 5. Prepare to send this transaction to operator as win
            }

            // record operator processing time
            httpRequestLog.setOperatorProcessStartTime(System.currentTimeMillis());
            if (!isBetExistsForUnsettledBet) {
                balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, betResultDataForOperator, resultType);
                cachingService.storePlayerLatestBalanceToRedis(gameSession, balanceVo.getData().getBalance());
            } else {
                // TODO: add try-catch in case operator fails
                balanceVo = walletBalanceAction.call(traceId, gameSession);
            }
            httpRequestLog.setOperatorProcessEndTime(System.currentTimeMillis());

            if (isSettled) {
                settledBet.setResultType(vendorService.calculateBetResultType(settledBet));
                long settledTime = System.currentTimeMillis();
                //get settle time from unsettled bet first
                if (unsettledBet.getVendorSettleTime() != null) {
                    settledTime = unsettledBet.getVendorSettleTime();
                }
                // but if bet result data have settle time then use it
                if (betResultData.getVendorSettleTime() != null) {
                    settledTime = betResultData.getVendorSettleTime();
                }
                settledBet.setVendorSettleTime(settledTime);
                settledBetService.create(settledBet, rawData);

                BetHistory betHistory = new BetHistory(settledBet);
                log.info(new Gson().toJson(betHistory));
                kafkaService.produceBetHistory(betHistory, settledBet);

                if (unsettledBetList != null && unsettledBetList.size() > 0) { // multiple bets within same round
                    for (UnsettledBet betRecord : unsettledBetList) {
                        if (!settledBet.getId().equals(betRecord.getId())) {
                            SettledBet newSettledBet = new SettledBet(betRecord, vendorService);
                            String newTraceId = UUID.randomUUID().toString();
                            newSettledBet.setInternalTransactionId(newTraceId);

                            newSettledBet.setResultType(vendorService.calculateBetResultType(newSettledBet));
                            newSettledBet.setVendorSettleTime(settledTime);

                            //AgentPlayerUsername, CurrencyCode and GameCode is used for walletBetResultAction.call when process end round result for operator
                            EndRoundSettledBet endRoundSettledBet = new EndRoundSettledBet(newSettledBet, gameSession.getAgentPlayerUsername(),
                                    gameSession.getCurrencyCode(), gameSession.getGameCode(), vendorService.calculateBetResultType(newSettledBet));

                            kafkaService.produceEndRoundSettleBet(endRoundSettledBet);
                        }

                        //no matter match or not, will perform delete unsettled bet data with same round Id
                        betHistoryService.deleteUnsettledBet(betRecord);
                    }
                    httpRequestLog.setOperatorProcessEndTime(System.currentTimeMillis());
                }
            } else { // Unsettled
                switch (resultType) {
                    case WIN, LOSE -> { // PP WIN
                        unsettledBet.setOperatorStatus(1);
                        unsettledBetService.update(unsettledBet);

                        // Create result_log record in couchbase for idempotent checks
                        betResultLogService.create(traceId, unsettledBet.getBetId(), betResultData, gameSession, balanceVo.getData().getBalance());
                    }
                    case BET_WIN, BET_LOSE -> betHistoryService.createUnsettledBet(unsettledBet);
                    default -> log.warn("ProcessBetResult.exception -> result not handled");
                }
            }

            //TODO: refine proper handle for result bet event
//            ResultBetEvent resultBetEvent = new ResultBetEvent(betInformation, balanceVo.getData().getBalance());

            // 5. Insert into couchbase unsettled_bet_results table
//            betResultLogService.create(unsettledBetResult);

            return balanceVo.getData().getBalance();

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            //TODO: To discuss if Agent is disable, should we just remove the session of this player and return vendor with invalid bet request?
//            resultBetOperatorFailEvent = new ResultBetOperatorFailEvent(unsettledBetResult, ResponseCodes.Status.SC_USER_DISABLED.code);
            throw invalidAgentApiCredentialException;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //TODO: if operator responses failed message, we should just move on and expect vendor resend?
//            resultBetOperatorFailEvent = new ResultBetOperatorFailEvent(unsettledBetResult, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

//        } catch (InsufficientBalanceException insufficientBalanceException) {
//            resultBetOperatorFailEvent = new ResultBetOperatorFailEvent(rawResultBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
//            throw insufficientBalanceException;

        } finally {
            boolean isOperatorFailed = resultBetOperatorFailEvent != null;
            if (isOperatorFailed) {
                // TODO: if operator failed, we just resend and does not need to update any status on unsettled bet, so eventing is not needed anymore
                //EventDispatcherSystem.emitAsync(unsettledBetOperatorFailEvent);
            }
            httpRequestLog.setBetProcessEndTime(System.currentTimeMillis());
        }
    }

    public BigDecimal processPromo(String traceId, GameSession gameSession, BetResultData betResultData, String rawData)
            throws InvalidAgentApiCredentialException, InvalidOperatorResponseException, BetResultIdempotentViolationException {

        this.idempotentCheckForBetResult(gameSession, betResultData);
        BigDecimal balance = this.getBalance(traceId, gameSession);
        balance = balance.add(betResultData.getWinAmount());
        betResultLogService.create(traceId, betResultData.getVendorBetId(), betResultData, gameSession, balance);

        return balance;
    }

    private void idempotentCheckForBetResult(GameSession gameSession, BetResultData betResultData) throws BetResultIdempotentViolationException {
        String transactionId = betResultData.getExternalTransactionId();
        String roundId = betResultData.getRoundId();
        String vendorGameId = gameSession.getVendorGameId().toString();
        String vendorPlayerId = gameSession.getVendorPlayerId().toString();

        RawBetResultLog rawBetResultLog = betResultLogService.checkExists(transactionId, roundId, vendorGameId, vendorPlayerId);

        if (rawBetResultLog != null) {

            BigDecimal newBalance = cachingService.getPlayerLatestBalanceFromRedis(gameSession).getBalance();
            rawBetResultLog.setBalance(Optional.ofNullable(newBalance).orElse(rawBetResultLog.getBalance()));

            BetResultIdempotentViolationException idempotentViolationException = new BetResultIdempotentViolationException();
            idempotentViolationException.setBetResultLog(rawBetResultLog);
            throw idempotentViolationException;
        }
    }

    private void mergeResultIntoBetData(BetInformation betData, BetResultData betResultData, ResultType resultType, String traceId) {
        // TODO: NEED INCLUDE WINAMOUNT AND JACKPOTAMOUNT TO CONSIDER THE INTERNALTRANSACTIONID?
//        if (!betData.getResultType().toString().equals(resultType.code.toString())) {
//            betData.setInternalTransactionId(traceId);
//        }
        // else remain with same transactionId;

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

                SettledBet newSettledBet = new SettledBet(unsettledBet, vendorService);
                newSettledBet.setInternalTransactionId(traceId);
                newSettledBet.setStatus(betStatus.code);
                newSettledBet.setVendorSettleTime(rollbackTimestamp);
                newSettledBet.setResultTime(rollbackTimestamp);
                newSettledBet.setResultType(ResultType.BET.code);
                newSettledBet.setEffectiveTurnover(BigDecimal.ZERO);
                newSettledBet.setWinLoss(BigDecimal.ZERO);

                settledBetService.create(newSettledBet, " ");
                BetHistory betHistory = new BetHistory(newSettledBet);
                log.info(new Gson().toJson(betHistory));
                kafkaService.produceBetHistory(betHistory, newSettledBet);

                RawBetRefundLog rawBetRefundLog = betRefundLogService.newRawBetRefundLog(traceId, betId, rollbackData, roundId, gameSession, balance);
                betRefundLogService.create(rawBetRefundLog);
                BetRefundLog betRefundLog = new BetRefundLog(rawBetRefundLog);
                log.info(new Gson().toJson(rawBetRefundLog));
                //TODO INSERT INTO KAFKA

                betHistoryService.deleteUnsettledBet(unsettledBet);
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

                    settledBetService.create(settledBet, " ");
                    BetHistory betHistory = new BetHistory(settledBet);
                    log.info(new Gson().toJson(betHistory));
                    kafkaService.produceBetHistory(betHistory, settledBet);
                }
            }
            default -> log.warn("processRollback.exception -> bet status not handled");
        }

        return balance;
    }

    private UnsettledBet newUnsettledBet(GameSession gameSession, String rawData,
                                         BetResultData betResultData, String traceId, Integer resultType) {

        UnsettledBet unsettledBet = new UnsettledBet();
//        String md5RawData = DigestUtils.md5Hex(rawData);

        unsettledBet.setId(betResultData.getVendorBetId() + '_' + betResultData.getRoundId() + '_' + gameSession.getVendorGameId() + '_' + gameSession.getVendorPlayerId());
        unsettledBet.setInternalTransactionId(traceId);
        unsettledBet.setBetId(traceId);
        unsettledBet.setExternalTransactionId(betResultData.getExternalTransactionId());
        unsettledBet.setRoundId(betResultData.getRoundId());
        unsettledBet.setVendorGameId(gameSession.getVendorGameId());
        unsettledBet.setVendorPlayerId(gameSession.getVendorPlayerId());
        unsettledBet.setVendorId(gameSession.getVendorId());
        unsettledBet.setAgentPlayerId(gameSession.getAgentPlayerId());
        unsettledBet.setAgentId(gameSession.getAgentId());
        unsettledBet.setVendorLineId(gameSession.getVendorLineId());
        unsettledBet.setGameCategoryId(gameSession.getGameCategoryId());
        unsettledBet.setCurrencyId(gameSession.getCurrencyId());
        unsettledBet.setBetAmount(betResultData.getBetAmount());
        unsettledBet.setGameSessionToken(gameSession.getToken());
        unsettledBet.setResultType(resultType);
        unsettledBet.setVendorBetTime(betResultData.getVendorBetTime());
        unsettledBet.setGameSessionToken(gameSession.getToken());
        unsettledBet.setOperatorStatus(1);
        unsettledBet.setRawData(rawData);
        unsettledBet.setWinAmount(betResultData.getWinAmount());
        unsettledBet.setWinLoss(betResultData.getWinLoss());
        unsettledBet.setEffectiveTurnover(betResultData.getEffectiveTurnover());
        unsettledBet.setVendorSettleTime(betResultData.getVendorSettleTime());
        unsettledBet.setResultTime(betResultData.getResultTime());
        unsettledBet.setVendorBetId(betResultData.getVendorBetId());
        unsettledBet.setJackpotAmount(betResultData.getJackpotAmount());
        unsettledBet.setIsFreespin(Optional.ofNullable(betResultData.getIsFreespin()).orElse(0));
        unsettledBet.setStatus(betResultData.getBetStatus().code);

        return unsettledBet;
    }
}
