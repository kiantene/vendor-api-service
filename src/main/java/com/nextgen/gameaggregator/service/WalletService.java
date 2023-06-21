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
            TransactionStillProcessingException, SettledBetIdempotentViolationException {

        String rawData = httpRequestLog.getRequestBody();
        Integer vendorId = gameSession.getVendorId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        Integer vendorGameId = gameSession.getVendorGameId();
        String vendorBetId = betResultData.getVendorBetId();
        String roundId = betResultData.getRoundId();
        Integer agentId = gameSession.getAgentId();
        BetInformation walletBetResultData = null;
        UnsettledBet unsettledBet = null;
        SettledBet settledBet = null;
        Integer statusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer statusSuccess = ResponseCodes.Status.SC_OK.code;
        WalletBalanceVo balanceVo = null;
        boolean retry = false;

        // check for idempotency in settled_bet
        try {
            settledBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(vendorBetId, roundId, vendorId, vendorPlayerId);

            if (settledBet != null) { // duplicate request found in settled_bet
                Integer operatorStatus = settledBet.getOperatorStatus();
                // throw idempotent exception if status is processing or success
                // if status is error, we will resend to Operator with same betId
                if (operatorStatus.equals(statusProcessing)) {
                    throw new TransactionStillProcessingException();

                } else if (operatorStatus.equals(statusSuccess)) {
                    SettledBetIdempotentViolationException idempotentViolationException = new SettledBetIdempotentViolationException();
                    idempotentViolationException.setSettledBet(settledBet);

                    throw idempotentViolationException;
                } else {
                    walletBetResultData = settledBet;
                    retry = true;
                }
            }
        } catch (BetNotFoundException betNotFoundException) {
            // proceed normally when bet not found
        }

        List<UnsettledBet> unsettledBetList = unsettledBetService.getByRoundId(roundId, vendorGameId, vendorPlayerId);

        if (!retry) {
            switch (resultType) {
                case LOSE, END -> { // PP

                    unsettledBet = this.getUnsettledBetFromRound(unsettledBetList, roundId);

                    // handle if settle end/lose resultType having isFreeSpin = 1.
                    unsettledBet.setIsFreespin((betResultData.getIsFreespin() == 1) ? betResultData.getIsFreespin() : unsettledBet.getIsFreespin());
                    settledBet = new SettledBet(unsettledBet, vendorService);
                    settledBet.setInternalTransactionId(traceId);
                    walletBetResultData = settledBet;
                }
                case WIN -> { // CQ9

                    unsettledBet = this.getUnsettledBetFromRound(unsettledBetList, roundId);
                    this.mergeResultIntoBetData(unsettledBet, betResultData, resultType, traceId);
                    settledBet = new SettledBet(unsettledBet, vendorService);
                    settledBet.setInternalTransactionId(traceId);

                    //do not send aggregated settledBet as betResultDataForOperator for settled and win scenario
                    walletBetResultData = new SettledBet(betResultData);
                    walletBetResultData.setBetAmount(BigDecimal.ZERO);
                    walletBetResultData.setInternalTransactionId(traceId);
                    walletBetResultData.setBetId(settledBet.getBetId());
                    walletBetResultData.setVendorBetTime(settledBet.getVendorBetTime());
                    walletBetResultData.setWinLoss(settledBet.getWinLoss());
                    walletBetResultData.setEffectiveTurnover(settledBet.getEffectiveTurnover());
                }
                case BET_WIN, BET_LOSE -> { // PGSoft

                    unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, traceId, resultType.code);
                    settledBet = new SettledBet(unsettledBet, vendorService);
                    walletBetResultData = settledBet;
                }
            }
        }

        // send bet data to Operator
        try {
            settledBet.setVendorSettleTime(betHistoryService.getVendorSettleTime(betResultData, unsettledBet));
            settledBet.setResultType(betHistoryService.getResultType(settledBet));
            settledBet.setOperatorStatus(statusProcessing);
//            settledBet.setVendorCurrencyCode(gameSession.getVendorCurrencyCode());
            settledBetService.save(settledBet, rawData);

            httpRequestLog.setOperatorProcessStartTime(System.currentTimeMillis());
            balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, walletBetResultData, resultType);
            httpRequestLog.setOperatorProcessEndTime(System.currentTimeMillis());

            cachingService.storePlayerLatestBalanceToRedis(gameSession, balanceVo.getData().getBalance());

            // update operator status after receiving response from operator
            settledBet.setOperatorStatus(statusSuccess);
            settledBet.setPlayerBalance(balanceVo.getData().getBalance());
            settledBetService.save(settledBet, rawData);

            // send settled bet to kafka
            BetHistory betHistory = new BetHistory(settledBet);
            kafkaService.produceBetHistory(betHistory, settledBet);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBet.setOperatorStatus(invalidOperatorResponseException.getOperatorStatus());
            // update operator status after receiving response from operator
            settledBetService.save(settledBet, rawData);
            throw invalidOperatorResponseException;
        }

        this.notifyEndRoundAsync(unsettledBetList, settledBet, vendorService, gameSession);

        return balanceVo;
    }

    private void notifyEndRoundAsync(List<UnsettledBet> unsettledBetList, SettledBet settledBet, BaseVendorService vendorService, GameSession gameSession) {
        if (unsettledBetList.size() > 0) { // multiple bets within same round
            for (UnsettledBet betRecord : unsettledBetList) {
                if (!settledBet.getId().equals(betRecord.getId())) { // exclude the current bet record
                    SettledBet newSettledBet = new SettledBet(betRecord, vendorService);
                    newSettledBet.setInternalTransactionId(UUID.randomUUID().toString());
                    newSettledBet.setResultType(betHistoryService.getResultType(newSettledBet));
                    newSettledBet.setVendorSettleTime(settledBet.getVendorSettleTime());

                    //AgentPlayerUsername, CurrencyCode and GameCode is used for walletBetResultAction.call when process end round result for operator
                    EndRoundSettledBet endRoundSettledBet = new EndRoundSettledBet(newSettledBet, gameSession.getAgentPlayerUsername(),
                            gameSession.getCurrencyCode(), gameSession.getGameCode(), betHistoryService.getResultType(newSettledBet));

                    kafkaService.produceEndRoundSettleBet(endRoundSettledBet);
                }

                //no matter match or not, will perform delete unsettled bet data with same round Id
                betHistoryService.deleteUnsettledBet(betRecord);
            }
        }
    }

//    private WalletBalanceVo doSettledBetRetry(String traceId, GameSession gameSession, SettledBet settledBet, ResultType resultType, HttpRequestLog httpRequestLog)
//        throws InvalidAgentApiCredentialException, InvalidOperatorResponseException {
//
//        Integer agentId = gameSession.getAgentId();
//        String rawData = httpRequestLog.getRequestBody();
//        WalletBalanceVo balanceVo;
//
//        try {
//            settledBet.setOperatorStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
//            settledBetService.save(settledBet, rawData);
//
//            httpRequestLog.setOperatorProcessStartTime(System.currentTimeMillis());
//            balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, settledBet, resultType);
//            httpRequestLog.setOperatorProcessEndTime(System.currentTimeMillis());
//
//            cachingService.storePlayerLatestBalanceToRedis(gameSession, balanceVo.getData().getBalance());
//
//            // update operator status after receiving response from operator
//            settledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
//            settledBet.setPlayerBalance(balanceVo.getData().getBalance());
//            settledBetService.save(settledBet, rawData);
//
//            // send settled bet to kafka
//            BetHistory betHistory = new BetHistory(settledBet);
//            kafkaService.produceBetHistory(betHistory, settledBet);
//
//        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
//            settledBet.setOperatorStatus(invalidOperatorResponseException.getOperatorStatus());
//            // update operator status after receiving response from operator
//            settledBetService.save(settledBet, rawData);
//            throw invalidOperatorResponseException;
//        }
//
//        return balanceVo;
//    }

    private WalletBalanceVo doUnsettledBetResult(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, BaseVendorService vendorService, HttpRequestLog httpRequestLog)
            throws BetNotFoundException, InvalidAgentApiCredentialException, InvalidOperatorResponseException, BetResultIdempotentViolationException {

        String rawData = httpRequestLog.getRequestBody();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        Integer vendorGameId = gameSession.getVendorGameId();
        String vendorBetId = betResultData.getVendorBetId();
        String roundId = betResultData.getRoundId();
        Integer agentId = gameSession.getAgentId();
        UnsettledBet unsettledBet = new UnsettledBet(betResultData);
        boolean isBetExistsForUnsettledBet = false;
        WalletBalanceVo balanceVo;
        Integer statusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer statusSuccess = ResponseCodes.Status.SC_OK.code;

        BetInformation walletBetResultData = unsettledBet;
        walletBetResultData.setInternalTransactionId(traceId);

        // Idempotent checks
        this.idempotentCheckForBetResult(gameSession, betResultData);

        // create bet result log first with betId = 0
        betResultLogService.create(traceId, "0", betResultData, gameSession, BigDecimal.ZERO, statusProcessing);

        switch (resultType) {
            case WIN, LOSE -> { // PP Win
                List<UnsettledBet> unsettledBetList = unsettledBetService.getByRoundId(roundId, vendorGameId, vendorPlayerId);

                unsettledBet = this.getUnsettledBetFromRound(unsettledBetList, roundId);
                this.mergeResultIntoBetData(unsettledBet, betResultData, resultType, traceId);
                unsettledBet.setWinLoss(vendorService.calculateWinLoss(unsettledBet));
                unsettledBet.setEffectiveTurnover(vendorService.calculateEffectiveTurnover(unsettledBet));

                walletBetResultData.setBetId(unsettledBet.getBetId());
                walletBetResultData.setVendorBetTime(unsettledBet.getVendorBetTime());
            }
            case BET_WIN, BET_LOSE -> {
                try {
                    unsettledBet = unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
                    boolean operatorHasError = unsettledBet.getOperatorStatus() > 1;
                    boolean isVendorPGSoft = gameSession.getVendorId() == 2;

                    // even if the unsettledBet is found, but if the operatorStatus is > 1 then will let the unsettledBet continue process
                    if (operatorHasError && isVendorPGSoft) {
                        unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, traceId, resultType.code);
                        walletBetResultData = unsettledBet;
                    } else {
                        isBetExistsForUnsettledBet = true;
                    }

                } catch (BetNotFoundException betNotFoundException) {
                    unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, traceId, resultType.code);
                    walletBetResultData = unsettledBet;
                }
            }
        }

        // create unsettledBet first with betId = 0
        unsettledBet.setOperatorStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        unsettledBetService.update(unsettledBet);

        // send bet data to Operator
        try {
            // record operator processing time
            httpRequestLog.setOperatorProcessStartTime(System.currentTimeMillis());
            if (!isBetExistsForUnsettledBet) {
                balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, walletBetResultData, resultType);
                cachingService.storePlayerLatestBalanceToRedis(gameSession, balanceVo.getData().getBalance());
            } else {
                // TODO: add try-catch in case operator fails
                balanceVo = walletBalanceAction.call(traceId, gameSession);
            }
            httpRequestLog.setOperatorProcessEndTime(System.currentTimeMillis());

            // Create result_log record in couchbase for idempotent checks
            betResultLogService.create(traceId, unsettledBet.getBetId(), betResultData, gameSession, balanceVo.getData().getBalance(), statusSuccess);

            unsettledBet.setOperatorStatus(statusSuccess);
            switch (resultType) {
                case WIN, LOSE -> unsettledBetService.update(unsettledBet);
                case BET_WIN, BET_LOSE -> betHistoryService.createUnsettledBet(unsettledBet);
            }
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {

            // record status code from operator if they return an error
            Integer operatorStatus = invalidOperatorResponseException.getOperatorStatus();
            betResultLogService.create(traceId, "0", betResultData, gameSession, BigDecimal.ZERO, operatorStatus);
            unsettledBet.setOperatorStatus(operatorStatus);
            unsettledBetService.update(unsettledBet);

            throw invalidOperatorResponseException;
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
     * @throws BetNotFoundException            If no bet record is found
     * @throws CouchbaseDataIntegrityException If anything wrong data inser into couchbase Id is found
     */
    public BigDecimal processBetResult(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, BaseVendorService vendorService, HttpRequestLog httpRequestLog)
            throws BetNotFoundException, InvalidOperatorResponseException,
            InvalidAgentApiCredentialException, MergedBetDataIntegrityException, InsufficientBalanceException,
            TransactionStillProcessingException, BetResultIdempotentViolationException, SettledBetIdempotentViolationException {

        httpRequestLog.setBetProcessStartTime(System.currentTimeMillis());
        log.info("processBetResult:" + resultType + " (" + traceId + ") :" + betResultData);
        ;
        WalletBalanceVo balanceVo;
        boolean isSettled = betResultData.getBetStatus().isValueOf(BetStatus.SETTLED.code);

        try {
            if (isSettled) {
                balanceVo = this.doSettledBetResult(traceId, gameSession, betResultData, resultType, vendorService, httpRequestLog);
            } else { // bets not settled yet
                balanceVo = this.doUnsettledBetResult(traceId, gameSession, betResultData, resultType, vendorService, httpRequestLog);
            }
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            //TODO: To discuss if Agent is disable, should we just remove the session of this player and return vendor with invalid bet request?
//            resultBetOperatorFailEvent = new ResultBetOperatorFailEvent(unsettledBetResult, ResponseCodes.Status.SC_USER_DISABLED.code);

            throw invalidAgentApiCredentialException;

        }
        httpRequestLog.setBetProcessEndTime(System.currentTimeMillis());

        return balanceVo.getData().getBalance();
    }

    public BigDecimal processPromo(String traceId, GameSession gameSession, BetResultData betResultData, String rawData)
            throws InvalidAgentApiCredentialException, InvalidOperatorResponseException, BetResultIdempotentViolationException {

        this.idempotentCheckForBetResult(gameSession, betResultData);
        BigDecimal balance = this.getBalance(traceId, gameSession);
        balance = balance.add(betResultData.getWinAmount());
        betResultLogService.create(traceId, betResultData.getVendorBetId(), betResultData, gameSession, balance, 1);

        return balance;
    }

    private void idempotentCheckForBetResult(GameSession gameSession, BetResultData betResultData) throws BetResultIdempotentViolationException {
        String transactionId = betResultData.getExternalTransactionId();
        String roundId = betResultData.getRoundId();
        String vendorGameId = gameSession.getVendorGameId().toString();
        String vendorPlayerId = gameSession.getVendorPlayerId().toString();

        RawBetResultLog rawBetResultLog = betResultLogService.checkExists(transactionId, roundId, vendorGameId, vendorPlayerId);

        if (rawBetResultLog != null) {
            Integer operatorStatus = rawBetResultLog.getOperatorStatus();
            boolean isOperatorSuccess = operatorStatus == 0 || operatorStatus == 1;

            if (isOperatorSuccess) {
                //here would have operatorStatus = 0 (data still processing) and operatorStatus = 1 (really idempotentViolation)
                BigDecimal newBalance = cachingService.getPlayerLatestBalanceFromRedis(gameSession).getBalance();
                rawBetResultLog.setBalance(Optional.ofNullable(newBalance).orElse(rawBetResultLog.getBalance()));

                BetResultIdempotentViolationException idempotentViolationException = new BetResultIdempotentViolationException();
                idempotentViolationException.setBetResultLog(rawBetResultLog);
                throw idempotentViolationException;
            }

            // if record exists and operator status is error, don't throw IdempotentViolation but forward the request to Operator
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

                settledBetService.save(newSettledBet, " ");
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

    private UnsettledBet newUnsettledBet(GameSession gameSession, String rawData,
                                         BetResultData betResultData, String traceId, Integer resultType) {

        UnsettledBet unsettledBet = new UnsettledBet();

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
