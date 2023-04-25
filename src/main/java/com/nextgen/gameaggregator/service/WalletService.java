package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetAction;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultAction;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackAction;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class WalletService {
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
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
    private WalletWinAction walletWinAction;
    @Autowired
    private WalletRollbackAction walletRollbackAction;
    @Autowired
    private CachingService cachingService;
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private Environment environment;
    @Autowired
    private WalletBetResultAction walletBetResultAction;
    @Autowired
    private KafkaService kafkaService;

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
    public BigDecimal processBet(String traceId, GameSession gameSession, BetResultData betResultData, String rawData) throws
            InsufficientBalanceException, CouchbaseDataIntegrityException, InvalidOperatorResponseException,
            InvalidAgentApiCredentialException {

        Integer agentId = gameSession.getAgentId();
        UnsettledBetOperatorFailEvent unsettledBetOperatorFailEvent = null;
        UnsettledBet unsettledBet = null;
        String vendorBetId = betResultData.getVendorBetId();
        String roundId = betResultData.getRoundId();
        Integer vendorGameId = gameSession.getVendorGameId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        boolean isBetExists = true;
        BigDecimal balance = BigDecimal.ZERO;

        try {
            unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
        } catch (BetNotFoundException betNotFoundException) {
            isBetExists = false;
        }

        // no record in database, will proceed to send as bet request and insert as unsettled_bet data to couchbase
        if (!isBetExists) {
            try {
                // 2. Generate rawUnsettledBet
                unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, traceId, ResultType.BET.code);

                WalletBalanceVo balanceVo = walletBetAction.call(traceId, agentId, gameSession, betResultData);
                UnsettledBetEvent unsettledBetEvent = new UnsettledBetEvent(unsettledBet, balanceVo.getData().getBalance());
                balance = balanceVo.getData().getBalance();
                // 5. Insert into couchbase unsettled_bet table
                betHistoryService.createUnsettledBet(unsettledBet);

                // TODO: if operator failed
                //EventDispatcherSystem.emitAsync(unsettledBetEvent);

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
        } else {
            // TODO: add try-catch in case operator fails
            balance = this.getBalance(traceId, gameSession);
        }

        return balance;
    }

    /**
     * To process the result of a bet by sending the bet result data to Operator so that the Operator can update
     * the player's balance.
     *
     * @param traceId       A unique Id for this request
     * @param gameSession   GameSession object containing information of the vendor, game, player
     * @param betResultData UnsettledResultSettledData object containing information of the bet result
     * @param rawData       Raw data sent by vendor containing information of the bet result
     * @return ResultBetEvent An event object containing Bet and Bet Result information as well as the last balance
     * that can be used for further processing, if required
     * @throws BetNotFoundException            If no bet record is found
     * @throws CouchbaseDataIntegrityException If anything wrong data inser into couchbase Id is found
     */
    public BigDecimal processBetResult(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, BaseVendorService vendorService, String rawData)
            throws BetNotFoundException, InvalidOperatorResponseException, CouchbaseDataIntegrityException,
            InvalidAgentApiCredentialException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = gameSession.getAgentId();
//        Integer vendorLineId = gameSession.getVendorLineId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String roundId = betResultData.getRoundId();
        String vendorBetId = betResultData.getVendorBetId();
        Integer vendorGameId = gameSession.getVendorGameId();
        Integer isFreeSpin = (ObjectUtils.isEmpty(betResultData.getIsFreespin())?0:betResultData.getIsFreespin());
        ResultBetOperatorFailEvent resultBetOperatorFailEvent = null;

        boolean isSettled = betResultData.getBetStatus().isValueOf(BetStatus.SETTLED.code);

        try {
            // 1. Retrieve unsettled bet transaction to prepare for merging with bet result
            UnsettledBet unsettledBet = null;
            SettledBet settledBet = null;
            BigDecimal winLoss, effectiveTurnover, jackpotAmount, winAmount;
            WalletBalanceVo balanceVo;
            BetInformation betInformation;
            boolean isBetExistsForUnsettledBet = false;

            if (isSettled) {
                switch (resultType) {
                    case LOSE, END -> { // PP END
                        unsettledBet = unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
                        settledBet = new SettledBet(unsettledBet);
                        settledBet.setStatus(BetStatus.SETTLED.code);
                        winAmount = settledBet.getWinAmount();
                        jackpotAmount = settledBet.getJackpotAmount();
                        winLoss = settledBet.getWinLoss();
                        effectiveTurnover = settledBet.getEffectiveTurnover();
                        settledBet.setInternalTransactionId(traceId);

                        if (winAmount == null) {
                            winAmount = vendorService.calculateWinAmount(settledBet);
                            settledBet.setWinAmount(winAmount);
                        }
                        if (jackpotAmount == null) {
                            jackpotAmount = vendorService.calculateJackpotAmount(settledBet);
                            settledBet.setJackpotAmount(jackpotAmount);
                        }
                        if (winLoss == null) {
                            winLoss = vendorService.calculateWinLoss(settledBet);
                            settledBet.setWinLoss(winLoss);
                        }
                        if (effectiveTurnover == null) {
                            effectiveTurnover = vendorService.calculateEffectiveTurnover(settledBet);
                            settledBet.setEffectiveTurnover(effectiveTurnover);
                        }
                    }
                    case WIN -> { // CQ9 Win
                        unsettledBet = unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
                        this.mergeResultIntoBetData(unsettledBet, betResultData, resultType, traceId);
                        winAmount = vendorService.calculateWinAmount(unsettledBet);
                        winLoss = vendorService.calculateWinLoss(unsettledBet);
                        effectiveTurnover = vendorService.calculateEffectiveTurnover(unsettledBet);
                        jackpotAmount = vendorService.calculateJackpotAmount(unsettledBet);

                        settledBet = new SettledBet(unsettledBet);
                        settledBet.setStatus(BetStatus.SETTLED.code);
                        settledBet.setWinAmount(winAmount);
                        settledBet.setWinLoss(winLoss);
                        settledBet.setEffectiveTurnover(effectiveTurnover);
                        settledBet.setJackpotAmount(jackpotAmount);
                    } // PGS
                    // PGS
                    case BET_WIN, BET_LOSE, BET_JACKPOT -> { // PGS
                        unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, traceId, resultType.code);
                        settledBet = new SettledBet(unsettledBet);
                        settledBet.setStatus(BetStatus.SETTLED.code);
                        winAmount = vendorService.calculateWinAmount(unsettledBet);
                        winLoss = vendorService.calculateWinLoss(settledBet);
                        effectiveTurnover = vendorService.calculateEffectiveTurnover(settledBet);
                        jackpotAmount = vendorService.calculateJackpotAmount(unsettledBet);
                        settledBet.setWinAmount(winAmount);
                        settledBet.setWinLoss(winLoss);
                        settledBet.setEffectiveTurnover(effectiveTurnover);
                        settledBet.setJackpotAmount(jackpotAmount);
                    }
                    default -> log.warn("ProcessBetResult.exception -> result not handled");
                }
//                unsettledBetService.update(unsettledBet);
                settledBet.setVendorSettleTime((ObjectUtils.isEmpty(settledBet.getVendorSettleTime())?betResultData.getVendorSettleTime():settledBet.getVendorSettleTime()));
                settledBet.setIsFreespin(isFreeSpin);

                betInformation = settledBet;
            } else { // bets not settled yet

                switch (resultType) {
                    case WIN, LOSE -> { // PP Win
                        // check if bet record exists
                        unsettledBet = unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
                        this.mergeResultIntoBetData(unsettledBet, betResultData, resultType, traceId);
                        winLoss = vendorService.calculateWinLoss(unsettledBet);
                        effectiveTurnover = vendorService.calculateEffectiveTurnover(unsettledBet);
                        unsettledBet.setWinLoss(winLoss);
                        unsettledBet.setEffectiveTurnover(effectiveTurnover);

                    }
                    case BET_WIN, BET_LOSE, BET_JACKPOT -> {
                        try {
                            unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
                            isBetExistsForUnsettledBet = true;
                        } catch (BetNotFoundException betNotFoundException) {
                            unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, traceId, resultType.code);
                        }
                    }

                    default -> log.warn("ProcessBetResult.exception -> result not handled");
                }
                unsettledBet.setIsFreespin((isFreeSpin));
                betInformation = unsettledBet;

                // insert into unsettled_bet_result
//                unsettledBetService.update(unsettledBet);
                // 5. Prepare to send this transaction to operator as win
            }

            if (!isBetExistsForUnsettledBet) {
                balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, betInformation, resultType);
            } else {
                // TODO: add try-catch in case operator fails
                balanceVo = walletBalanceAction.call(traceId, gameSession);
            }

            if (isSettled) {
                settledBet.setResultType(vendorService.calculateBetResultType(settledBet));
                settledBetService.create(settledBet, rawData);
                BetHistory betHistory = new BetHistory(settledBet);
                kafkaService.produceBetHistory(betHistory);

                List<SettledBet> settledBetLists = new ArrayList<>();
                settledBetLists = settledBetService.getAllUnsettledBetsWithSameRoundId(settledBet.getRoundId(), settledBet.getVendorLineId(),
                        settledBet.getVendorPlayerId(), settledBet.getVendorSettleTime());

                if (settledBetLists.isEmpty()) {
                    //which mean dont have any roundId of this bet is still in unsettled_bet table, then do nothing
                } else {
                    //then need process all unsettled_bet of this roundId to operator and send to kafka
                    for (SettledBet settledBetList : settledBetLists) {
                        betInformation = settledBetList;
                        traceId = UUID.randomUUID().toString();
                        walletBetResultAction.call(traceId, agentId, gameSession, betInformation, ResultType.END);

                        settledBetList.setResultType(vendorService.calculateBetResultType(settledBetList));
                        settledBetService.create(settledBetList, settledBetList.getRawData());
                        betHistory = new BetHistory(settledBetList);
                        kafkaService.produceBetHistory(betHistory);
                    }
                }

            } else {
                switch (resultType) {
                    case WIN -> unsettledBetService.update(unsettledBet);
                    case BET_WIN, BET_LOSE, BET_JACKPOT -> betHistoryService.createUnsettledBet(unsettledBet);
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
        }
    }

    private void mergeResultIntoBetData(BetInformation betData, BetResultData betResultData, ResultType resultType, String traceId) {
        // TODO: NEED INCLUDE WINAMOUNT AND JACKPOTAMOUNT TO CONSIDER THE INTERNALTRANSACTIONID?
        if(!betData.getResultType().toString().equals(resultType.code.toString())){
            betData.setInternalTransactionId(traceId);
        }
        // else remain with same transactionId;
        betData.setResultType(resultType.code);

        BigDecimal winAmount = Optional.ofNullable(betData.getWinAmount()).orElse(BigDecimal.ZERO);
        BigDecimal winAmountLatest = Optional.ofNullable(betResultData.getWinAmount()).orElse(BigDecimal.ZERO);
        BigDecimal finalWinAmount = winAmount.stripTrailingZeros().toPlainString().equals(winAmountLatest.stripTrailingZeros().toPlainString())?winAmount:winAmount.add(winAmountLatest);
        betData.setWinAmount(finalWinAmount);

        BigDecimal jackpotAmount = Optional.ofNullable(betData.getJackpotAmount()).orElse(BigDecimal.ZERO);
        BigDecimal jackpotAmountLatest = Optional.ofNullable(betResultData.getJackpotAmount()).orElse(BigDecimal.ZERO);
        BigDecimal finalJackpotAmount = jackpotAmount.stripTrailingZeros().toPlainString().equals(jackpotAmountLatest.stripTrailingZeros().toPlainString())?jackpotAmount:jackpotAmount.add(jackpotAmountLatest);
        betData.setJackpotAmount(finalJackpotAmount);

        Integer isFreeSpin = Optional.ofNullable(betData.getIsFreespin()).orElse(0);
        betData.setIsFreespin(isFreeSpin);
        betData.setResultTime(betResultData.getResultTime());
        betData.setVendorSettleTime(betResultData.getVendorSettleTime());
    }

    /**
     * To process the result of a bet by sending the bet result data to Operator so that the Operator can update
     * the player's balance.
     *
     * @param traceId     A unique Id for this request
     * @param gameSession gameSession object containing information of the vendor, game, player
     * @param winData     SettledData object containing information of the bet result
     * @param rawData     Raw data sent by vendor containing information of the bet result
     * @return BetResultEvent An event object containing Bet and Bet Result information as well as the last balance
     * that can be used for further processing, if required
     * @throws BetNotFoundException                    If no bet record is found
     * @throws DuplicateExternalTransactionIdException If vendor's transaction Id is found
     */
//    public BetResultEvent processWin(String traceId, com.nextgen.gameaggregator.entity.GameSession gameSession, WinData winData, String rawData) throws
//            BetNotFoundException, DuplicateExternalTransactionIdException, InvalidOperatorResponseException, BetResultNotFoundException {
//
//        Integer agentId = gameSession.getAgentId();
//        Integer vendorGameId = gameSession.getVendorGameId();
//        Long vendorPlayerId = gameSession.getVendorPlayerId();
//        String roundId = winData.getRoundId();
//
//        // 1. Retrieve the bet transaction
//        BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(roundId, vendorGameId, vendorPlayerId);
//
//        WalletWinDto walletWinDto = this.newWalletWinDto(traceId, gameSession, winData, betHistory.getId());
//
//        BetResultLog betResultLog = this.newBetResultLog(traceId, gameSession, winData, betHistory, walletWinDto, rawData);
//        BetResultEvent betResultEvent = null;
//        Boolean requiredCallOperator = true;
//
//        try {
//            betResultLog = winData.prepareData(betHistory, betResultLog);
//            betResultLogService.create(betResultLog);
//        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
//
//            Integer getOperatorStatus = betHistoryService.getBetHistoryByExternalTransaction(betResultLog.getExternalTransactionId(), betResultLog.getRoundId(),
//                    betResultLog.getVendorLineId()).getOperatorStatus();
//
//            if (betResultLog.getOperatorStatus() == 1) {
//                betResultEvent = new BetResultEvent(betHistory, betResultLog, BigDecimal.ZERO);
//                requiredCallOperator = false;
//            } else {
//                betResultLog.setOperatorStatus(getOperatorStatus);
//            }
//        }
//
//        //IF the bet ID is duplicated and not error, will return as 0 to vendor
//        if (requiredCallOperator) {
//            // TODO: To discuss if Agent is disable, should system ignore callback and just insert to bet_result_log
//            try {
//                AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
//                WalletBalanceVo balanceVo = walletWinAction.call(agentApiCredential, walletWinDto);
//                betResultEvent = new BetResultEvent(betHistory, betResultLog, balanceVo.getData().getBalance());
//
//            } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
//                betResultEvent = new BetResultEvent(betHistory, betResultLog, BigDecimal.ZERO);
//                //Update bet_result_log operator status to agent is disable
//                BetResultOperatorFailEvent betResultOperatorFailEvent =
//                        new BetResultOperatorFailEvent(betResultLog, -1);
//                EventDispatcherSystem.emitAsync(betResultOperatorFailEvent);
//
//            } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
//                //Update bet_result_log operator status based on exception
//                BetResultOperatorFailEvent betResultOperatorFailEvent =
//                        new BetResultOperatorFailEvent(betResultLog, invalidOperatorResponseException.getOperatorStatus());
//                EventDispatcherSystem.emitAsync(betResultOperatorFailEvent);
//                throw invalidOperatorResponseException;
//            }
//
//            EventDispatcherSystem.emitAsync(betResultEvent);
//        }
//
//        return betResultEvent;
//
//    }

    /**
     * To process the reversal of a bet by sending the rollback instruction to Operator so that the Operator can perform
     * a reversal and return the updated balance of the player.
     *
     * @param traceId               A unique Id for this request
     * @param externalTransactionId Vendor's bet transaction Id of a previous bet record
     * @param gameSession           gameSession object containing information of the vendor, game, player
     * @param rawData               Raw data sent by vendor containing information of the Refund
     * @return BetRefundEvent An event object containing Bet and Refund information to be used for further processing, if required
     * @throws BetNotFoundException    If no bet record is found
     * @throws RecordNotFoundException Generic exception for orphan records
     */
    public BetRollbackEvent processRollback(String traceId, String externalTransactionId, GameSession gameSession, String rawData)
            throws RecordNotFoundException, InvalidAgentApiCredentialException, InvalidOperatorResponseException {

        Integer agentId = gameSession.getAgentId();
        Integer vendorId = gameSession.getVendorId();
        BetRollbackEvent betRollbackEvent = null;

        try {
            UnsettledBet unsettledBet = unsettledBetService.getByVendorIdAndExternalTransactionId(vendorId, externalTransactionId);
            WalletBalanceVo balanceVo = walletRollbackAction.call(traceId, agentId, gameSession, unsettledBet.getId());
            betRollbackEvent = new BetRollbackEvent(null, unsettledBet, null, balanceVo.getData().getBalance());

        } catch (BetNotFoundException betNotFoundException) {
            // TODO: bet not found in unsettled bets table, need to search settled_bets
            log.warn("processRollback -> BetNotFoundException: vendorId (" + vendorId + ") externalTransactionId (" + externalTransactionId + ")");
        }

//        BetRefundLog betRefundLog = this.newBetRefundLog(betHistory, externalTransactionId, currentTimestamp, rawData);
//
//        BetRollbackEvent betRollbackEvent = null;
//        Boolean requiredCallOperator = true;
//        try {
//            betRefundLogService.create(betRefundLog);
//        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
//
//            System.err.println(externalTransactionId);
//            System.err.println(betHistory.getRoundId());
//            System.err.println(gameSession.getVendorLineId());
//            BetRefundLog currentBetRefundLog = betRefundLogService.findByExternalTransactionIdAndRoundIdAndVendorLineId(
//                    externalTransactionId, betHistory.getRoundId(), gameSession.getVendorLineId());
//
//
//            if (currentBetRefundLog.getOperatorStatus() == 1) {
//                betRollbackEvent = new BetRollbackEvent(betHistory, betRefundLog, BigDecimal.ZERO);
//                requiredCallOperator = false;
//            } else {
//                betRefundLog.setId(currentBetRefundLog.getId());
//                betRefundLog.setOperatorStatus(currentBetRefundLog.getOperatorStatus());
//            }
//
//        }
//        if (requiredCallOperator) {
//            // TODO: ok To discuss if Agent is disable, should system ignore callback and just insert to bet_result_log
//            try {
//                AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
//                WalletBalanceVo balanceVo = walletRollbackAction.call(traceId, agentApiCredential, walletRollbackDto);
//
//                betRollbackEvent = new BetRollbackEvent(betHistory, betRefundLog, balanceVo.getData().getBalance());
//
//            } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
//                betRollbackEvent = new BetRollbackEvent(betHistory, betRefundLog, BigDecimal.ZERO);
//                //Update bet_refund_log operator status to agent is disable
//                BetRefundOperatorFailEvent betRefundOperatorFailEvent =
//                        new BetRefundOperatorFailEvent(betRefundLog, -1);
//                EventDispatcherSystem.emitAsync(betRefundOperatorFailEvent);
//
//            } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
//                //Update bet_result_log operator status based on exception
//                BetRefundOperatorFailEvent betRefundOperatorFailEvent =
//                        new BetRefundOperatorFailEvent(betRefundLog, invalidOperatorResponseException.getOperatorStatus());
//                EventDispatcherSystem.emitAsync(betRefundOperatorFailEvent);
//                throw invalidOperatorResponseException;
//            }
//            // Emit event for additional asynchronous processing such as publishing data to a kafka topic
//            EventDispatcherSystem.emitAsync(betRollbackEvent);
//        }


        // TODO: to refactor currency
        return betRollbackEvent;
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
//        unsettledBet.setRefundAmount(betResultData.getRefundAmount());
        unsettledBet.setVendorSettleTime(betResultData.getVendorSettleTime());
        unsettledBet.setResultTime(betResultData.getResultTime());
        unsettledBet.setVendorBetId(betResultData.getVendorBetId());
        unsettledBet.setJackpotAmount(betResultData.getJackpotAmount());
        unsettledBet.setIsFreespin(betResultData.getIsFreespin());
        unsettledBet.setStatus(betResultData.getBetStatus().code);

        return unsettledBet;
    }

//    private WalletWinDto newWalletWinDto(String traceId, com.nextgen.gameaggregator.entity.GameSession gameSession, WinData winData, String referenceTransactionId) {
//        WalletWinDto walletWinDto = new WalletWinDto();
//        walletWinDto.setTraceId(traceId);
//        walletWinDto.setTransactionId(traceId);
//        walletWinDto.setUsername(gameSession.getAgentPlayerUsername());
//        walletWinDto.setCurrency(gameSession.getCurrencyCode());
//        walletWinDto.setToken(gameSession.getToken());
//        walletWinDto.setExternalTransactionId(winData.getExternalTransactionId());
//        walletWinDto.setReferenceTransactionId(referenceTransactionId);
//        walletWinDto.setAmount(winData.getAmount());
//        walletWinDto.setGameCode(gameSession.getGameCode());
//        walletWinDto.setRoundId(winData.getRoundId());
//        walletWinDto.setResultType(winData.getWinType());
//        walletWinDto.setTimestamp(winData.getTimestamp());
//        return walletWinDto;
//    }

//    private BetResultLog newBetResultLog(String traceId, com.nextgen.gameaggregator.entity.GameSession gameSession, WinData winData, BetHistory betHistory, WalletWinDto walletWinDto, String rawData) {
//        BetResultLog betResultLog = new BetResultLog();
//
//        betResultLog.setId(traceId);
//        betResultLog.setBetHistoryId(walletWinDto.getReferenceTransactionId());
//        betResultLog.setExternalTransactionId(walletWinDto.getExternalTransactionId());
//        betResultLog.setRoundId(betHistory.getRoundId());
//        betResultLog.setVendorGameId(gameSession.getVendorGameId());
//        betResultLog.setVendorPlayerId(gameSession.getVendorPlayerId());
//        betResultLog.setAgentPlayerId(gameSession.getAgentPlayerId());
//        betResultLog.setAgentId(gameSession.getAgentId());
//        betResultLog.setVendorLineId(gameSession.getVendorLineId());
//        betResultLog.setCurrencyId(gameSession.getCurrencyId());
//        betResultLog.setOperatorStatus(1);
//        betResultLog.setWinAmount(walletWinDto.getAmount());
//        betResultLog.setEffectiveTurnover(winData.getEffectiveTurnover());
//        betResultLog.setResultType(winData.getWinType().code);
//        //TODO remove the balance column from bet_result_log table
//        betResultLog.setBalance(BigDecimal.ZERO);
//        betResultLog.setRawData(rawData);
//        betResultLog.setVendorTime(walletWinDto.getTimestamp());
//
//        return betResultLog;
//    }

    private BetRefundLog newBetRefundLog(BetHistory betHistory, String externalTransactionId, Long currentTimestamp, String rawData) {
        BetRefundLog betRefundLog = new BetRefundLog();

        betRefundLog.setBetHistoryId(betHistory.getId());
        betRefundLog.setExternalTransactionId(externalTransactionId);
        betRefundLog.setRoundId(betHistory.getRoundId());
        betRefundLog.setVendorGameId(betHistory.getVendorGameId());
        betRefundLog.setVendorPlayerId(betHistory.getVendorPlayerId());
        betRefundLog.setAgentPlayerId(betHistory.getAgentPlayerId());
        betRefundLog.setAgentId(betHistory.getAgentId());
        betRefundLog.setCurrencyId(betHistory.getCurrencyId());
        //TODO remove the balance column from bet_refund_log table
        betRefundLog.setBalance(BigDecimal.ZERO);
        betRefundLog.setVendorLineId(betHistory.getVendorLineId());
        betRefundLog.setRawData(rawData);
        betRefundLog.setOperatorStatus(1);
        betRefundLog.setStatus(1); // TODO: refactor, map to constant/enum value
        betRefundLog.setCreateTime(currentTimestamp);

        return betRefundLog;
    }
}
