package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceDto;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetAction;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetDto;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultAction;
import com.nextgen.gameaggregator.operator.wallet.refund.WalletRefundAction;
import com.nextgen.gameaggregator.operator.wallet.refund.WalletRefundDto;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinAction;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
    private WalletRefundAction walletRefundAction;
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
                unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, traceId);

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
        ResultBetOperatorFailEvent resultBetOperatorFailEvent = null;

        boolean isSettled = betResultData.getBetStatus().isValueOf(BetStatus.SETTLED.code);

        try {
            // 1. Retrieve unsettled bet transaction to prepare for merging with bet result
            UnsettledBet unsettledBet = null;
            SettledBet settledBet = null;
            BigDecimal winLoss, effectiveTurnover;
            WalletBalanceVo balanceVo;
            BetInformation betInformation;
            boolean isBetExistsForUnsettledBet = false;

            if (isSettled) {
                switch (resultType) {
                    case END -> { // PP END
                        unsettledBet = unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
                        settledBet = new SettledBet(unsettledBet);
                        settledBet.setStatus(BetStatus.SETTLED.code);
                        winLoss = settledBet.getWinLoss();
                        effectiveTurnover = settledBet.getEffectiveTurnover();

                        if (winLoss == null) {
                            winLoss = vendorService.calculateWinLoss(settledBet);
                            settledBet.setWinLoss(winLoss);
                        }
                        if (effectiveTurnover == null) {
                            effectiveTurnover = vendorService.calculateEffectiveTurnover(settledBet);
                            settledBet.setEffectiveTurnover(effectiveTurnover);
                        }
                    }
                    case WIN, JACKPOT -> { // CQ9 Win
                        unsettledBet = unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
                        this.mergeResultIntoBetData(unsettledBet, betResultData, resultType);
                        winLoss = vendorService.calculateWinLoss(unsettledBet);
                        effectiveTurnover = vendorService.calculateEffectiveTurnover(unsettledBet);
                        settledBet = new SettledBet(unsettledBet);
                        settledBet.setStatus(BetStatus.SETTLED.code);
                        settledBet.setWinLoss(winLoss);
                        settledBet.setEffectiveTurnover(effectiveTurnover);
                    } // PGS
                    // PGS
                    case BET_WIN, BET_LOSE, BET_JACKPOT -> { // PGS
                        unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, traceId);
                        settledBet = new SettledBet(unsettledBet);
                        settledBet.setStatus(BetStatus.SETTLED.code);
                        winLoss = vendorService.calculateWinLoss(settledBet);
                        effectiveTurnover = vendorService.calculateEffectiveTurnover(settledBet);
                        settledBet.setWinLoss(winLoss);
                        settledBet.setEffectiveTurnover(effectiveTurnover);
                    }
                    default -> log.warn("ProcessBetResult.exception -> result not handled");
                }
//                unsettledBetService.update(unsettledBet);
                betInformation = settledBet;
            } else { // bets not settled yet

                switch (resultType) {
                    case WIN, JACKPOT -> { // PP Win
                        // check if bet record exists
                        unsettledBet = unsettledBetService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
                        this.mergeResultIntoBetData(unsettledBet, betResultData, resultType);
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
                            unsettledBet = this.newUnsettledBet(gameSession, rawData, betResultData, traceId);
                        }
                    }

                    default -> log.warn("ProcessBetResult.exception -> result not handled");
                }
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
                // TODO: to change to publish kafka
                settledBetService.create(settledBet, rawData);
                // publish to kafka
//                kafkaService.produceBetHistory(settledBet);
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

    private void mergeResultIntoBetData(BetInformation betData, BetResultData betResultData, ResultType resultType) {
        // TODO: need to validate original bet data is in sync with betResult's bet data
        betData.setResultType(resultType.code);

        BigDecimal winAmount = Optional.ofNullable(betData.getWinAmount()).orElse(BigDecimal.ZERO);
        BigDecimal winAmountLatest = Optional.ofNullable(betResultData.getWinAmount()).orElse(BigDecimal.ZERO);
        betData.setWinAmount(winAmount.add(winAmountLatest));

        BigDecimal jackpotAmount = Optional.ofNullable(betData.getJackpotAmount()).orElse(BigDecimal.ZERO);
        BigDecimal jackpotAmountLatest = Optional.ofNullable(betResultData.getJackpotAmount()).orElse(BigDecimal.ZERO);
        betData.setJackpotAmount(jackpotAmount.add(jackpotAmountLatest));

        betData.setIsFreespin(betResultData.getIsFreespin());
        betData.setResultTime(betResultData.getResultTime());
        betData.setVendorSettleTime(betResultData.getVendorSettleTime());
    }

    /**
     * To process the full bet (unsettle + result + end round) and getting the balance of player from operator
     *
     * @param traceId       A unique Id for this request
     * @param gameSession   GameSession object containing information of the vendor, game, player
     * @param betResultData UnsettledResultSettledData object containing information of the full bet details
     * @param rawData       String body of entire vendor request params.
     * @return The player's current wallet
     */
    public SettledBetEvent processBetResultPlus(String traceId, GameSession gameSession, BetResultData betResultData, ResultType resultType, String rawData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = gameSession.getAgentId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;

        // 1. Generate rawSettledBet
        SettledBet settledBet = this.newSettledBet(traceId, gameSession, betResultData, rawData);

//        System.out.println("rawSettledBet = " + settledBet);

        try {
            // 2. Prepare to send this transaction to operator, with isFullBet as true
            WalletBalanceVo balanceVo = walletBetResultAction.call(traceId, agentId, gameSession, settledBet, null);
            SettledBetEvent settledBetEvent = new SettledBetEvent(settledBet, balanceVo.getData().getBalance());

            // 3. Insert into couchbase settled table (and mariaDB if testing stub is disabled)
//            System.out.println("rawSettledBet = " + settledBet);
//            System.out.println("BetStatus.UNSETTLED.code = " + BetStatus.UNSETTLED.code);
//            System.out.println("rawSettledBet.getStatus() = " + settledBet.getStatus());

            // 3. Process data into couchbase
            if (BetStatus.UNSETTLED.isValueOf(settledBet.getStatus())) {
                // create unsettled bet record and store to unsettledBet couchbase
                UnsettledBet unsettledBet = this.toUnsettleBet(settledBet);
                betHistoryService.createUnsettledBet(unsettledBet);

            } else { // Status is settled
                // merge and create list of settled record of same round id bet to kafka
                List<SettledBet> settledBetLists = settledBetService.getBetResultListData(settledBet);

                for (SettledBet rawUnsettledBetList : settledBetLists) {
                    BetHistory betHistory = this.toBetHistory(rawUnsettledBetList);
                    kafkaService.produceBetHistory(betHistory);
                }
            }

            return settledBetEvent;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(settledBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

//        } catch (InsufficientBalanceException insufficientBalanceException) {
//            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(settledBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
//            throw insufficientBalanceException;

        } finally {
            boolean isOperatorFailed = settledBetOperatorFailEvent != null;
            if (isOperatorFailed) {
                // TODO: if operator failed, we just resend and does not need to update any status on unsettled bet, so eventing is not needed anymore
                //EventDispatcherSystem.emitAsync(unsettledBetOperatorFailEvent);
            }
        }
    }

    /**
     * To process the settled bet by merging unsettled and result bet and getting the balance of player from operator
     * When the Operator has responded with sufficient balance, we will save a record of the bet
     * as Unsettled.
     *
     * @param traceId       A unique Id for this request
     * @param gameSession   GameSession object containing information of the vendor, game, player
     * @param betResultData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @return The player's current wallet
     */
    public SettledBetEvent processSettledBet(String traceId, GameSession gameSession, BetResultData betResultData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException {

        Integer agentId = gameSession.getAgentId();
        Integer vendorLineId = gameSession.getVendorLineId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String roundId = betResultData.getRoundId();
        String vendorBetId = betResultData.getVendorBetId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;
        Integer vendorGameId = gameSession.getVendorGameId();
        BigDecimal transferAmount = (betResultData.getWinLoss() == null) ? BigDecimal.valueOf(0) : betResultData.getWinLoss();

        // 1. Retrieve unsettled bet from couchbase
        UnsettledBet unsettledBet = betHistoryService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);

        // 2. Retrieve result bet from couchbase
        UnsettledBetResult unsettledBetResult = betResultLogService.getRawResultBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 3. Generate settled bet with end round bet data
        SettledBet settledBet = this.newSettledBet(gameSession, betResultData);

        // 4. Combine unsettled bet, result bet and end round bet data into settle bet
        settledBet = settledBetService.updateRawSettledBet(unsettledBet, unsettledBetResult, settledBet);

        // 5. Prepare wallet settled dto
        WalletWinDto walletWinDto = this.newWalletResultDto(traceId, gameSession, betResultData, unsettledBet.getId(), transferAmount, settledBet.getResultTime());

        // 6. Insert into couchbase settled_bet table and also mariadb
//        settledBetService.createSettledBet(rawSettledBet);
//        boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
//        if (stub == false) {
//            settledBetService.createSettleBetMariaDB(rawSettledBet);
//        }

        BetHistory betHistory = this.toBetHistory(settledBet);
        kafkaService.produceBetHistory(betHistory);

        try {
            // 7. Prepare to send this transaction to operator as win
            AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
            WalletBalanceVo balanceVo = walletWinAction.call(agentApiCredential, walletWinDto);

            // 8. prepare the async event to flush cache from redis and couchbase
            SettledBetEvent settledBetEvent = new SettledBetEvent(settledBet, balanceVo.getData().getBalance());
            EventDispatcherSystem.emitAsync(settledBetEvent);

            return settledBetEvent;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(settledBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } finally {
            boolean isOperatorFailed = settledBetOperatorFailEvent != null;
            if (isOperatorFailed) {
                // TODO: if operator failed, we just resend and does not need to update any status on unsettled bet, so eventing is not needed anymore
                //EventDispatcherSystem.emitAsync(unsettledBetOperatorFailEvent);
            }
        }
    }

    /**
     * To process the settled bet by merging unsettled and result bet and getting the balance of player from operator
     * When the Operator has responded with sufficient balance, we will save a record of the bet
     * as Unsettled.
     *
     * @param traceId       A unique Id for this request
     * @param gameSession   GameSession object containing information of the vendor, game, player
     * @param betResultData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @return entire SettledBetEvent in case vendor need more than balance from the process flow.
     */
    public SettledBetEvent processResultSettle(String traceId, GameSession gameSession, BetResultData betResultData, String rawData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = gameSession.getAgentId();
        Integer vendorLineId = gameSession.getVendorLineId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String roundId = betResultData.getRoundId();
        String vendorBetId = betResultData.getVendorBetId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;

        // 1. Retrieve the rawUnsettledBet bet data
        UnsettledBet unsettledBet = betHistoryService.getUnsettledBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 2. Generate rawSettledBet
        SettledBet settledBet = this.newSettledBet(unsettledBet.getInternalTransactionId(), gameSession, betResultData, rawData);

        // 3. Combine rawUnsettledBet and rawSettledBet into final rawSettledBet data
        settledBet = settledBetService.updateRawSettledBet(unsettledBet, null, settledBet);

        // 4. Insert into couchbase settled table (and mariaDB if testing stub is disabled)
//        settledBetService.createSettledBet(rawSettledBet);
//        boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
//        if (stub == false) {
//            settledBetService.createSettleBetMariaDB(rawSettledBet);
//        }

        BetHistory betHistory = this.toBetHistory(settledBet);
        kafkaService.produceBetHistory(betHistory);

        try {
            // 5. Prepare to send this transaction to operator with isFullBet is false
            WalletBalanceVo balanceVo = this.sendSettledWalletTransaction(agentId, traceId, gameSession, settledBet, false);
            SettledBetEvent settledBetEvent = new SettledBetEvent(settledBet, balanceVo.getData().getBalance());

            // 6. Create async thread to flush rawUnsettledBet in couchbase and redis
            EventDispatcherSystem.emitAsync(settledBetEvent);

            return settledBetEvent;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(settledBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(settledBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            throw insufficientBalanceException;

        } finally {
            boolean isOperatorFailed = settledBetOperatorFailEvent != null;
            if (isOperatorFailed) {
                // TODO: if operator failed, we just resend and does not need to update any status on unsettled bet, so eventing is not needed anymore
                //EventDispatcherSystem.emitAsync(unsettledBetOperatorFailEvent);
            }
        }
    }

    /**
     * To process the full bet (unsettle + result + end round) and getting the balance of player from operator
     *
     * @param traceId       A unique Id for this request
     * @param gameSession   GameSession object containing information of the vendor, game, player
     * @param betResultData UnsettledResultSettledData object containing information of the full bet details
     * @param rawData       String body of entire vendor request params.
     * @return The player's current wallet
     */
    public SettledBetEvent processUnsettleResultSettle(String traceId, GameSession gameSession, BetResultData betResultData, String rawData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = gameSession.getAgentId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;

        // 1. Generate rawSettledBet
        SettledBet settledBet = this.newSettledBet(traceId, gameSession, betResultData, rawData);

        // 2. Insert into couchbase settled table (and mariaDB if testing stub is disabled)
//        settledBetService.createSettledBet(rawSettledBet);
//        boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
//        if (stub == false) {
//            settledBetService.createSettleBetMariaDB(rawSettledBet);
//        }

        BetHistory betHistory = this.toBetHistory(settledBet);
        kafkaService.produceBetHistory(betHistory);

        try {
            // 3. Prepare to send this transaction to operator, with isFullBet as true
            WalletBalanceVo balanceVo = this.sendSettledWalletTransaction(agentId, traceId, gameSession, settledBet, true);
            SettledBetEvent settledBetEvent = new SettledBetEvent(settledBet, balanceVo.getData().getBalance());

            // TODO: if operator failed, we just resend and does not need to update any status on unsettled bet, so eventing is not needed anymore
            //EventDispatcherSystem.emitAsync(settledBetEvent);

            return settledBetEvent;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(settledBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(settledBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            throw insufficientBalanceException;

        } finally {
            boolean isOperatorFailed = settledBetOperatorFailEvent != null;
            if (isOperatorFailed) {
                // TODO: if operator failed, we just resend and does not need to update any status on unsettled bet, so eventing is not needed anymore
                //EventDispatcherSystem.emitAsync(unsettledBetOperatorFailEvent);
            }
        }
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
    public BetRefundEvent processRollback(String traceId, String externalTransactionId, GameSession gameSession, String rawData) throws
            BetNotFoundException, RecordNotFoundException, InvalidAgentApiCredentialException, DuplicateExternalTransactionIdException, InvalidOperatorResponseException {

        Integer agentId = gameSession.getAgentId();
        Integer vendorId = gameSession.getVendorId();
        Long currentTimestamp = System.currentTimeMillis();
        Long vendorPlayerId = gameSession.getVendorPlayerId();

        // 1. Retrieve the bet transaction
        BetHistory betHistory = betHistoryService.getBetTransactionByVendorTransactionIdPlayerId(externalTransactionId, vendorId, vendorPlayerId);

        WalletRefundDto walletRefundDto = this.newWalletRefundDto(traceId, gameSession, currentTimestamp, betHistory);

        BetRefundLog betRefundLog = this.newBetRefundLog(betHistory, externalTransactionId, currentTimestamp, rawData);

        BetRefundEvent betRefundEvent = null;
        Boolean requiredCallOperator = true;
        try {
            betRefundLogService.create(betRefundLog);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {

            System.err.println(externalTransactionId);
            System.err.println(betHistory.getRoundId());
            System.err.println(gameSession.getVendorLineId());
            BetRefundLog currentBetRefundLog = betRefundLogService.findByExternalTransactionIdAndRoundIdAndVendorLineId(
                    externalTransactionId, betHistory.getRoundId(), gameSession.getVendorLineId());


            if (currentBetRefundLog.getOperatorStatus() == 1) {
                betRefundEvent = new BetRefundEvent(betHistory, betRefundLog, BigDecimal.ZERO);
                requiredCallOperator = false;
            } else {
                betRefundLog.setId(currentBetRefundLog.getId());
                betRefundLog.setOperatorStatus(currentBetRefundLog.getOperatorStatus());
            }

        }
        if (requiredCallOperator) {
            // TODO: ok To discuss if Agent is disable, should system ignore callback and just insert to bet_result_log
            try {
                AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
                WalletBalanceVo balanceVo = walletRefundAction.call(agentApiCredential, walletRefundDto);

                betRefundEvent = new BetRefundEvent(betHistory, betRefundLog, balanceVo.getData().getBalance());

            } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
                betRefundEvent = new BetRefundEvent(betHistory, betRefundLog, BigDecimal.ZERO);
                //Update bet_refund_log operator status to agent is disable
                BetRefundOperatorFailEvent betRefundOperatorFailEvent =
                        new BetRefundOperatorFailEvent(betRefundLog, -1);
                EventDispatcherSystem.emitAsync(betRefundOperatorFailEvent);

            } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
                //Update bet_result_log operator status based on exception
                BetRefundOperatorFailEvent betRefundOperatorFailEvent =
                        new BetRefundOperatorFailEvent(betRefundLog, invalidOperatorResponseException.getOperatorStatus());
                EventDispatcherSystem.emitAsync(betRefundOperatorFailEvent);
                throw invalidOperatorResponseException;
            }
            // Emit event for additional asynchronous processing such as publishing data to a kafka topic
            EventDispatcherSystem.emitAsync(betRefundEvent);
        }


        // TODO: to refactor currency
        return betRefundEvent;
    }

    private WalletBetDto newWalletBetDto(String traceId, com.nextgen.gameaggregator.entity.GameSession gameSession, BetData betData) {
        WalletBetDto walletBetDto = new WalletBetDto();
        walletBetDto.setTraceId(traceId);
        walletBetDto.setTransactionId(traceId);
        walletBetDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBetDto.setCurrency(gameSession.getCurrencyCode());
        walletBetDto.setToken(gameSession.getToken());
        walletBetDto.setExternalTransactionId(betData.getExternalTransactionId());
        walletBetDto.setAmount(betData.getAmount());
        walletBetDto.setGameCode(gameSession.getGameCode());
        walletBetDto.setRoundId(betData.getRoundId());
        walletBetDto.setTimestamp(betData.getTimestamp());

        return walletBetDto;
    }

    private BetHistory newBetHistory(WalletBetDto walletBetDto, com.nextgen.gameaggregator.entity.GameSession
            gameSession, String rawData) {
        BetHistory betHistory = new BetHistory();
        betHistory.setId(walletBetDto.getTraceId());
        betHistory.setExternalTransactionId(walletBetDto.getExternalTransactionId());
        betHistory.setRoundId(walletBetDto.getRoundId());
        betHistory.setVendorGameId(gameSession.getVendorGameId());
        betHistory.setVendorPlayerId(gameSession.getVendorPlayerId());
        betHistory.setVendorId(gameSession.getVendorId());
        betHistory.setAgentPlayerId(gameSession.getAgentPlayerId());
        betHistory.setAgentId(gameSession.getAgentId());
        betHistory.setVendorLineId(gameSession.getVendorLineId());
        betHistory.setGameCategoryId(gameSession.getGameCategoryId());
        betHistory.setCurrencyId(gameSession.getCurrencyId());
        betHistory.setBetAmount(walletBetDto.getAmount());
        betHistory.setGameSessionToken(gameSession.getToken());
        betHistory.setRawData(rawData);
        betHistory.setVendorBetTime(walletBetDto.getTimestamp());
        betHistory.setGameSessionToken(gameSession.getToken());
        betHistory.setOperatorStatus(1);

        return betHistory;
    }

    private UnsettledBet newUnsettledBet(GameSession gameSession, String rawData,
                                         BetResultData betResultData, String traceId) {

        UnsettledBet unsettledBet = new UnsettledBet();
        String md5RawData = DigestUtils.md5Hex(rawData);

        unsettledBet.setId(betResultData.getVendorBetId() + '_' + betResultData.getRoundId() + '_' + gameSession.getVendorGameId() + '_' + gameSession.getVendorPlayerId());
        unsettledBet.setInternalTransactionId(traceId);
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
        unsettledBet.setResultType(BetResultType.BET.code);
        unsettledBet.setVendorBetTime(betResultData.getVendorBetTime());
        unsettledBet.setGameSessionToken(gameSession.getToken());
        unsettledBet.setOperatorStatus(1);
//        unsettledBet.setMd5RawSettledResult(md5RawData);
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

    private WalletWinDto newWalletResultDto(String traceId, GameSession gameSession, BetResultData betResultData,
                                            String referenceTransactionId, BigDecimal transferAmount, Long timestamp) {

        WalletWinDto walletWinDto = new WalletWinDto();
        BigDecimal winAmount = (betResultData.getWinAmount() == null) ? BigDecimal.valueOf(0) : betResultData.getWinAmount();

        walletWinDto.setTraceId(traceId);
        walletWinDto.setTransactionId(traceId);
        walletWinDto.setUsername(gameSession.getAgentPlayerUsername());
        walletWinDto.setCurrency(gameSession.getCurrencyCode());
        walletWinDto.setToken(gameSession.getToken());
        walletWinDto.setExternalTransactionId(betResultData.getExternalTransactionId());
        walletWinDto.setReferenceTransactionId(referenceTransactionId);
        walletWinDto.setAmount((transferAmount == null) ? betResultData.getWinAmount() : transferAmount);
        walletWinDto.setGameCode(gameSession.getGameCode());
        walletWinDto.setRoundId(betResultData.getRoundId());
        walletWinDto.setResultType((winAmount.compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.LOSE);
        walletWinDto.setTimestamp(timestamp);

        return walletWinDto;
    }

    private WalletWinDto newWalletWinDtoForFullBetDto(String traceId, GameSession gameSession, SettledBet settledBet, BigDecimal transferAmount) {

        BigDecimal winAmount = (settledBet.getWinAmount() == null) ? BigDecimal.valueOf(0) : settledBet.getWinAmount();

        WalletWinDto walletWinDto = new WalletWinDto();
        walletWinDto.setTraceId(traceId);
        walletWinDto.setTransactionId(traceId);
        walletWinDto.setUsername(gameSession.getAgentPlayerUsername());
        walletWinDto.setCurrency(gameSession.getCurrencyCode());
        walletWinDto.setToken(gameSession.getToken());
        walletWinDto.setExternalTransactionId(settledBet.getExternalTransactionId());
        walletWinDto.setReferenceTransactionId(settledBet.getInternalTransactionId());
        walletWinDto.setAmount(transferAmount);
        walletWinDto.setGameCode(gameSession.getGameCode());
        walletWinDto.setRoundId(settledBet.getRoundId());
        walletWinDto.setResultType((winAmount.compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.LOSE);
        walletWinDto.setTimestamp(settledBet.getVendorBetTime());
        return walletWinDto;
    }

    private WalletBetDto newWalletBetForFullBetDto(String traceId, GameSession gameSession, SettledBet settledBet) {
        WalletBetDto walletBetDto = new WalletBetDto();
        walletBetDto.setTraceId(traceId);
        walletBetDto.setTransactionId(traceId);
        walletBetDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBetDto.setCurrency(gameSession.getCurrencyCode());
        walletBetDto.setToken(gameSession.getToken());
        walletBetDto.setExternalTransactionId(settledBet.getExternalTransactionId());
        walletBetDto.setAmount(settledBet.getWinLoss().abs());
        walletBetDto.setGameCode(gameSession.getGameCode());
        walletBetDto.setRoundId(settledBet.getRoundId());
        walletBetDto.setTimestamp(settledBet.getVendorBetTime());

        return walletBetDto;
    }

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

    private UnsettledBetResult newUnsettledBetResult(GameSession gameSession, BetResultData betResultData, ResultType resultType,
                                                     String rawData, UnsettledBet unsettledBet) {

        UnsettledBetResult unsettledBetResult = new UnsettledBetResult();
        String md5RawData = DigestUtils.md5Hex(rawData);
//        BigDecimal winLoss = unsettledResultSettledData.getWinAmount().subtract(rawUnsettledBet.getBetAmount());

        unsettledBetResult.setId(betResultData.getVendorBetId() + '_' + betResultData.getRoundId() + '_' + gameSession.getVendorLineId() + '_' + gameSession.getVendorPlayerId());
//        rawResultBet.setInternalTransactionId(rawUnsettledBet.getInternalTransactionId());
        unsettledBetResult.setExternalTransactionId(betResultData.getExternalTransactionId());
        unsettledBetResult.setRoundId(betResultData.getRoundId());
        unsettledBetResult.setVendorGameId(gameSession.getVendorGameId());
        unsettledBetResult.setVendorPlayerId(gameSession.getVendorPlayerId());
        unsettledBetResult.setAgentPlayerId(gameSession.getAgentPlayerId());
        unsettledBetResult.setAgentId(gameSession.getAgentId());
        unsettledBetResult.setVendorLineId(gameSession.getVendorLineId());
        unsettledBetResult.setCurrencyId(gameSession.getCurrencyId());
        unsettledBetResult.setOperatorStatus(1);
        unsettledBetResult.setWinAmount(betResultData.getWinAmount());
        unsettledBetResult.setEffectiveTurnover(betResultData.getBetAmount());
        unsettledBetResult.setWinLoss(betResultData.getWinLoss());
        unsettledBetResult.setResultType(resultType.code);
        unsettledBetResult.setMd5RawSettledResult(md5RawData);
        unsettledBetResult.setResultTime(betResultData.getResultTime());
        unsettledBetResult.setVendorSettleTime(betResultData.getVendorSettleTime());
        unsettledBetResult.setRefundAmount(BigDecimal.ZERO);
        unsettledBetResult.setBetAmount(betResultData.getBetAmount());
        unsettledBetResult.setJackpotAmount(betResultData.getJackpotAmount());
        unsettledBetResult.setVendorBetId(betResultData.getVendorBetId());
        unsettledBetResult.setIsFreespin(betResultData.getIsFreespin());
        unsettledBetResult.setStatus(betResultData.getBetStatus().code);

        return unsettledBetResult;
    }

    private SettledBet newSettledBet(GameSession gameSession, BetResultData betResultData)
            throws MergedBetDataIntegrityException {

        try {
            SettledBet settledBet = new SettledBet();
            BeanUtils.copyProperties(settledBet, betResultData);

            settledBet.setId(betResultData.getVendorBetId() + '_' + betResultData.getRoundId() + '_' + gameSession.getVendorLineId() + '_' + gameSession.getVendorPlayerId());
            settledBet.setVendorGameId(gameSession.getVendorGameId());
            settledBet.setVendorPlayerId(gameSession.getVendorPlayerId());
            settledBet.setAgentPlayerId(gameSession.getAgentPlayerId());
            settledBet.setAgentId(gameSession.getAgentId());
            settledBet.setVendorLineId(gameSession.getVendorLineId());
            settledBet.setCurrencyId(gameSession.getCurrencyId());
            settledBet.setStatus(betResultData.getBetStatus().code);

            return settledBet;

        } catch (IllegalAccessException illegalAccessException) {
            throw new MergedBetDataIntegrityException("getValueFromObject invalid : " + illegalAccessException.getMessage());

        } catch (InvocationTargetException invocationTargetException) {
            throw new MergedBetDataIntegrityException("copyProperties invalid : " + invocationTargetException.getMessage());
        }

    }

    private SettledBet newSettledBet(String traceId, GameSession gameSession,
                                     BetResultData betResultData, String rawData) {

        SettledBet settledBet = new SettledBet();
        String md5RawData = DigestUtils.md5Hex(rawData);

        settledBet.setId(betResultData.getVendorBetId() + '_' + betResultData.getRoundId() + '_' + gameSession.getVendorLineId() + '_' + gameSession.getVendorPlayerId());
        settledBet.setInternalTransactionId(traceId);
        settledBet.setExternalTransactionId(betResultData.getExternalTransactionId());
        settledBet.setRoundId(betResultData.getRoundId());
        settledBet.setVendorGameId(gameSession.getVendorGameId());
        settledBet.setVendorPlayerId(gameSession.getVendorPlayerId());
        settledBet.setVendorId(gameSession.getVendorId());
        settledBet.setVendorLineId(gameSession.getVendorLineId());
        settledBet.setAgentPlayerId(gameSession.getAgentPlayerId());
        settledBet.setAgentId(gameSession.getAgentId());
//        settledBet.setGameCategoryId(gameSession.getGameCategoryId());
        settledBet.setCurrencyId(gameSession.getCurrencyId());
        settledBet.setBetAmount(betResultData.getBetAmount());
        settledBet.setWinAmount(betResultData.getWinAmount());
        settledBet.setWinLoss(betResultData.getWinLoss());
        settledBet.setEffectiveTurnover(betResultData.getEffectiveTurnover());
//        settledBet.setRefundAmount(betResultData.getRefundAmount());
        settledBet.setResultType(BetResultType.BET.code);
//        settledBet.setMd5RawSettledResult(md5RawData);
//        settledBet.setResettleNum(0);
        settledBet.setGameSessionToken(gameSession.getToken());
        settledBet.setVendorBetTime(betResultData.getVendorBetTime());
        settledBet.setVendorSettleTime(betResultData.getVendorSettleTime());
        settledBet.setResultTime(betResultData.getResultTime());
        settledBet.setVendorBetId(betResultData.getVendorBetId());
        settledBet.setIsFreespin(betResultData.getIsFreespin());
        settledBet.setJackpotAmount(betResultData.getJackpotAmount());
        settledBet.setStatus(betResultData.getBetStatus().code);

        return settledBet;
    }

    private WalletRefundDto newWalletRefundDto(String traceId, com.nextgen.gameaggregator.entity.GameSession
            gameSession, Long currentTimestamp, BetHistory betHistory) {
        WalletRefundDto walletRefundDto = new WalletRefundDto();
        walletRefundDto.setTraceId(traceId);
        walletRefundDto.setTransactionId(traceId);
        walletRefundDto.setUsername(gameSession.getAgentPlayerUsername());
        walletRefundDto.setExternalTransactionId(betHistory.getExternalTransactionId());
        walletRefundDto.setReferenceTransactionId(betHistory.getId());
        walletRefundDto.setGameCode(gameSession.getGameCode());
        walletRefundDto.setRoundId(betHistory.getRoundId());
        walletRefundDto.setTimestamp(currentTimestamp);
        return walletRefundDto;
    }

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

    private WalletBalanceVo sendSettledWalletTransaction(Integer agentId, String traceId, GameSession gameSession, SettledBet settledBet, Boolean isFullBet)
            throws InvalidAgentApiCredentialException, InvalidOperatorResponseException, InsufficientBalanceException {

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        WalletBalanceVo balanceVo;

        if (isFullBet == true) {
            //if isFullBet = true, then we will compare with using winLoss to decide send as wallet/win or lose
            if ((settledBet.getWinLoss().compareTo(BigDecimal.ZERO) >= 0)) {
                //if WinLoss >= 0 then we will send as win
                WalletWinDto walletWinDto = this.newWalletWinDtoForFullBetDto(traceId, gameSession, settledBet, settledBet.getWinLoss());
                balanceVo = walletWinAction.call(agentApiCredential, walletWinDto);

            } else {
                //else send as lose
                WalletBetDto walletBetDto = this.newWalletBetForFullBetDto(traceId, gameSession, settledBet);
//                balanceVo = walletBetAction.call(agentApiCredential, walletBetDto);
                balanceVo = null;
            }
        } else {
            //else isFullBet = false, then we will send as wallet/win with winAmount (because bet already deducted)
            WalletWinDto walletWinDto = this.newWalletWinDtoForFullBetDto(traceId, gameSession, settledBet, settledBet.getWinAmount());
            balanceVo = walletWinAction.call(agentApiCredential, walletWinDto);
        }

        return balanceVo;
    }

    private BetHistory toBetHistory(SettledBet settledBet) throws MergedBetDataIntegrityException {

        try {
            BetHistory betHistory = new BetHistory();
            BeanUtils.copyProperties(betHistory, settledBet);
//            betHistory.setRawData(settledBet.getMd5RawSettledResult());
            //TODO REMOVING OPERATORSTATUS
            betHistory.setOperatorStatus(1);
            betHistory.setId(settledBet.getInternalTransactionId());
            betHistory.setCreateTime(Instant.now().toEpochMilli());

            return betHistory;

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new MergedBetDataIntegrityException("copyProperties invalid : " + e.getMessage());
        }
    }

    private UnsettledBet toUnsettleBet(SettledBet settledBet) throws MergedBetDataIntegrityException {

        try {
            UnsettledBet unsettledBet = new UnsettledBet();
            BeanUtils.copyProperties(unsettledBet, settledBet);

            return unsettledBet;

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new MergedBetDataIntegrityException("toUnsettleBet copyProperties invalid : " + e.getMessage());
        }
    }
}
