package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.WinType;
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
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultDto;
import com.nextgen.gameaggregator.operator.wallet.refund.WalletRefundAction;
import com.nextgen.gameaggregator.operator.wallet.refund.WalletRefundDto;
import com.nextgen.gameaggregator.operator.wallet.settled.UnsettledResultSettledData;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinAction;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinDto;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
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
    private SettledBetService settledBetService;
    @Autowired
    private Environment environment;
    @Autowired
    private WalletBetResultAction walletBetResultAction;
    @Autowired
    private KafkaService kafkaService;

    public BigDecimal getBalance(String traceId, RawGameSession rawGameSession) throws InvalidOperatorResponseException, InvalidAgentApiCredentialException {
        Integer agentId = rawGameSession.getAgentId();
        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);

        WalletBalanceDto walletBalanceDto = this.newWalletBalanceDto(traceId, rawGameSession);

        WalletBalanceVo balanceVo = walletBalanceAction.call(agentApiCredential, walletBalanceDto);
        // TODO: to handle balance returned with more than 4 decimals
        // TODO: implement error handling
        return balanceVo.getData().getBalance();
    }

    /**
     * To process the bet by sending the bet data to Operator to validate the player has sufficient balance
     * to place the bet.
     * <p>
     * When the Operator has responded with sufficient balance, we will save a record of the bet
     * as Unsettled.
     *
     * @param traceId     A unique Id for this request
     * @param rawGameSession RawGameSession object containing information of the vendor, game, player
     * @param betData     BetData object containing information of the bet such as betAmount, game, betTime
     * @param rawData     Raw data sent by vendor containing information of the bet
     * @return The player's current wallet balance after deducting the bet amount
     */
    public BetEvent processBet(String traceId, RawGameSession rawGameSession, BetData betData, String rawData) throws
            InsufficientBalanceException, DuplicateExternalTransactionIdException,
            InvalidOperatorResponseException, InvalidAgentApiCredentialException {

        Integer agentId = rawGameSession.getAgentId();
        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        WalletBetDto walletBetDto = this.newWalletBetDto(traceId, rawGameSession, betData);

        BetHistory betHistory = this.newBetHistory(walletBetDto, rawGameSession, rawData);
        betHistoryService.create(betHistory);
        BetOperatorFailEvent betOperatorFailEvent = null;

        try {
//            WalletBalanceVo balanceVo = walletBetAction.stub();
            WalletBalanceVo balanceVo = walletBetAction.call(agentApiCredential, walletBetDto);
            BetEvent betEvent = new BetEvent(betHistory, balanceVo.getData().getBalance());
            // TODO: check for null pointer
            // Emit event for additional asynchronous processing
            EventDispatcherSystem.emitAsync(betEvent);

            return betEvent;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            betOperatorFailEvent = new BetOperatorFailEvent(betHistory, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            throw insufficientBalanceException;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            betOperatorFailEvent = new BetOperatorFailEvent(betHistory, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } finally {
            boolean isOperatorFailed = betOperatorFailEvent != null;
            if (isOperatorFailed) {
                // emit operator failed event to update operator status
                EventDispatcherSystem.emitAsync(betOperatorFailEvent);
            }
        }
    }

    /**
     * To process the unsettled bet by sending the bet data to Operator to validate the player has sufficient balance
     * to place the bet.
     * <p>
     * When the Operator has responded with sufficient balance, we will save a record of the bet
     * as Unsettled.
     *
     * @param traceId                    A unique Id for this request
     * @param rawGameSession                RawGameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @param rawData                    Raw data sent by vendor containing information of the bet
     * @return The player's current wallet balance after deducting the bet amount
     */
    public UnsettledBetEvent processUnsettledBet(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            InsufficientBalanceException, CouchbaseDataIntegrityException, InvalidOperatorResponseException,
            InvalidAgentApiCredentialException {

        Integer agentId = rawGameSession.getAgentId();
        UnsettledBetOperatorFailEvent unsettledBetOperatorFailEvent = null;

        // 1. Generate walletBetDto
        WalletBetDto walletBetDto = this.newWalletUnsettledBetDto(traceId, rawGameSession, unsettledResultSettledData);

        // 2. Generate rawUnsettledBet
        RawUnsettledBet rawUnsettledBet = this.newUnsettledBet(rawGameSession, rawData, unsettledResultSettledData, traceId);

        try {
            // 3. Prepare callback info
            AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
            WalletBalanceVo balanceVo = walletBetAction.call(agentApiCredential, walletBetDto);

            UnsettledBetEvent unsettledBetEvent = new UnsettledBetEvent(rawUnsettledBet, balanceVo.getData().getBalance());

            // 5. Insert into couchbase unsettled_bet table
            betHistoryService.createUnsettledBet(rawUnsettledBet);

            // TODO: if operator failed
            //EventDispatcherSystem.emitAsync(unsettledBetEvent);

            return unsettledBetEvent;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            unsettledBetOperatorFailEvent = new UnsettledBetOperatorFailEvent(rawUnsettledBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            throw insufficientBalanceException;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            unsettledBetOperatorFailEvent = new UnsettledBetOperatorFailEvent(rawUnsettledBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } finally {
            boolean isOperatorFailed = unsettledBetOperatorFailEvent != null;
            if (isOperatorFailed) {
                // TODO: if operator failed, we just resend and does not need to update any status on unsettled bet, so eventing is not needed anymore
                //EventDispatcherSystem.emitAsync(unsettledBetOperatorFailEvent);
            }
        }
    }

    /**
     * To process the unsettled bet by sending the bet data to Operator to validate the player has sufficient balance
     * to place the bet.
     * <p>
     * When the Operator has responded with sufficient balance, we will save a record of the bet
     * as Unsettled.
     *
     * @param traceId                    A unique Id for this request
     * @param rawGameSession                RawGameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @param rawData                    Raw data sent by vendor containing information of the bet
     * @return The player's current wallet balance after deducting the bet amount
     */
    public UnsettledBetEvent processUnsettledBetPlus(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            InsufficientBalanceException, CouchbaseDataIntegrityException, InvalidOperatorResponseException,
            InvalidAgentApiCredentialException, MergedBetDataIntegrityException {

        Integer agentId = rawGameSession.getAgentId();
        UnsettledBetOperatorFailEvent unsettledBetOperatorFailEvent = null;

        // 1. Generate rawUnsettledBet
        RawUnsettledBet rawUnsettledBet = this.newUnsettledBet(rawGameSession, rawData, unsettledResultSettledData, traceId);

        try {
            // 2. Prepare callback info
            RawSettledBet rawSettledBet = settledBetService.convertRawUnsettledBetForWalletTransaction(rawUnsettledBet);
            WalletBalanceVo balanceVo = this.sendSettledWalletTransactionPlus(agentId, traceId, rawGameSession, rawSettledBet);

            UnsettledBetEvent unsettledBetEvent = new UnsettledBetEvent(rawUnsettledBet, balanceVo.getData().getBalance());

            // 3. Insert into couchbase unsettled_bet table
            betHistoryService.createUnsettledBet(rawUnsettledBet);

            // TODO: if operator failed
            //EventDispatcherSystem.emitAsync(unsettledBetEvent);

            return unsettledBetEvent;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            unsettledBetOperatorFailEvent = new UnsettledBetOperatorFailEvent(rawUnsettledBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            throw insufficientBalanceException;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            unsettledBetOperatorFailEvent = new UnsettledBetOperatorFailEvent(rawUnsettledBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } finally {
            boolean isOperatorFailed = unsettledBetOperatorFailEvent != null;
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
     * @param traceId                    A unique Id for this request
     * @param rawGameSession                RawGameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @return The player's current wallet
     */
    public SettledBetEvent processSettledBet(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException {

        Integer agentId = rawGameSession.getAgentId();
        Integer vendorLineId = rawGameSession.getVendorLineId();
        Long vendorPlayerId = rawGameSession.getVendorPlayerId();
        String roundId = unsettledResultSettledData.getRoundId();
        String vendorBetId = unsettledResultSettledData.getVendorBetId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;
        BigDecimal transferAmount = (unsettledResultSettledData.getWinLoss() == null) ? BigDecimal.valueOf(0) : unsettledResultSettledData.getWinLoss();

        // 1. Retrieve unsettled bet from couchbase
        RawUnsettledBet rawUnsettledBet = betHistoryService.getRawUnsettledBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 2. Retrieve result bet from couchbase
        RawResultBet rawResultBet = betResultLogService.getRawResultBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 3. Generate settled bet with end round bet data
        RawSettledBet rawSettledBet = this.newSettledBet(rawGameSession, unsettledResultSettledData);

        // 4. Combine unsettled bet, result bet and end round bet data into settle bet
        rawSettledBet = settledBetService.updateRawSettledBet(rawUnsettledBet, rawResultBet, rawSettledBet);

        // 5. Prepare wallet settled dto
        WalletWinDto walletWinDto = this.newWalletResultDto(traceId, rawGameSession, unsettledResultSettledData, rawUnsettledBet.getId(), transferAmount, rawSettledBet.getResultTime());

        // 6. Insert into couchbase settled_bet table and also mariadb
//        settledBetService.createSettledBet(rawSettledBet);
//        boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
//        if (stub == false) {
//            settledBetService.createSettleBetMariaDB(rawSettledBet);
//        }

        BetHistory betHistory = this.toBetHistory(rawSettledBet);
        kafkaService.produceBetHistory(betHistory);


        try {
            // 7. Prepare to send this transaction to operator as win
            AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
            WalletBalanceVo balanceVo = walletWinAction.call(agentApiCredential, walletWinDto);

            // 8. prepare the async event to flush cache from redis and couchbase
            SettledBetEvent settledBetEvent = new SettledBetEvent(rawSettledBet, balanceVo.getData().getBalance());
            EventDispatcherSystem.emitAsync(settledBetEvent);

            return settledBetEvent;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(rawSettledBet, invalidOperatorResponseException.getOperatorStatus());
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
     * @param traceId                    A unique Id for this request
     * @param rawGameSession                RawGameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @return The player's current wallet
     */
    public SettledBetEvent processSettledBetPlus(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = rawGameSession.getAgentId();
        Integer vendorLineId = rawGameSession.getVendorLineId();
        Long vendorPlayerId = rawGameSession.getVendorPlayerId();
        String roundId = unsettledResultSettledData.getRoundId();
        String vendorBetId = unsettledResultSettledData.getVendorBetId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;

        // 1. Retrieve unsettled bet from couchbase
        RawUnsettledBet rawUnsettledBet = betHistoryService.getRawUnsettledBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 2. Retrieve result bet from couchbase
        RawResultBet rawResultBet = betResultLogService.getRawResultBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 3. Generate settled bet with end round bet data
        RawSettledBet rawSettledBet = this.newSettledBet(rawGameSession, unsettledResultSettledData);

        // 4. Combine unsettled bet, result bet and end round bet data into settle bet
        rawSettledBet = settledBetService.updateRawSettledBet(rawUnsettledBet, rawResultBet, rawSettledBet);

        try {
            // 5. Prepare to send this transaction to operator
            WalletBalanceVo balanceVo = this.sendSettledWalletTransactionPlus(agentId, traceId, rawGameSession, rawSettledBet);

            BetHistory betHistory = this.toBetHistory(rawSettledBet);
            kafkaService.produceBetHistory(betHistory);

            // 6. Insert into couchbase settled_bet table and also mariadb
//            settledBetService.createSettledBet(rawSettledBet);
//            boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
//            if (stub == false) {
//                settledBetService.createSettleBetMariaDB(rawSettledBet);
//            }

            // 8. prepare the async event to flush cache from redis and couchbase
            SettledBetEvent settledBetEvent = new SettledBetEvent(rawSettledBet, balanceVo.getData().getBalance());
            EventDispatcherSystem.emitAsync(settledBetEvent);

            return settledBetEvent;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(rawSettledBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(rawSettledBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
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
     * To process the settled bet by merging unsettled and result bet and getting the balance of player from operator
     * When the Operator has responded with sufficient balance, we will save a record of the bet
     * as Unsettled.
     *
     * @param traceId                    A unique Id for this request
     * @param rawGameSession                RawGameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @return entire SettledBetEvent in case vendor need more than balance from the process flow.
     */
    public SettledBetEvent processResultSettle(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = rawGameSession.getAgentId();
        Integer vendorLineId = rawGameSession.getVendorLineId();
        Long vendorPlayerId = rawGameSession.getVendorPlayerId();
        String roundId = unsettledResultSettledData.getRoundId();
        String vendorBetId = unsettledResultSettledData.getVendorBetId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;

        // 1. Retrieve the rawUnsettledBet bet data
        RawUnsettledBet rawUnsettledBet = betHistoryService.getRawUnsettledBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 2. Generate rawSettledBet
        RawSettledBet rawSettledBet = this.newUnsettleResultSettledBet(rawUnsettledBet.getInternalTransactionId(), rawGameSession, unsettledResultSettledData, rawData);

        // 3. Combine rawUnsettledBet and rawSettledBet into final rawSettledBet data
        rawSettledBet = settledBetService.updateRawSettledBet(rawUnsettledBet, null, rawSettledBet);

        // 4. Insert into couchbase settled table (and mariaDB if testing stub is disabled)
//        settledBetService.createSettledBet(rawSettledBet);
//        boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
//        if (stub == false) {
//            settledBetService.createSettleBetMariaDB(rawSettledBet);
//        }

        BetHistory betHistory = this.toBetHistory(rawSettledBet);
        kafkaService.produceBetHistory(betHistory);

        try {
            // 5. Prepare to send this transaction to operator with isFullBet is false
            WalletBalanceVo balanceVo = this.sendSettledWalletTransaction(agentId, traceId, rawGameSession, rawSettledBet, false);
            SettledBetEvent settledBetEvent = new SettledBetEvent(rawSettledBet, balanceVo.getData().getBalance());

            // 6. Create async thread to flush rawUnsettledBet in couchbase and redis
            EventDispatcherSystem.emitAsync(settledBetEvent);

            return settledBetEvent;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(rawSettledBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(rawSettledBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
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
     * To process the settled bet by merging unsettled and result bet and getting the balance of player from operator
     * When the Operator has responded with sufficient balance, we will save a record of the bet
     * as Unsettled.
     *
     * @param traceId                    A unique Id for this request
     * @param rawGameSession                RawGameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @return entire SettledBetEvent in case vendor need more than balance from the process flow.
     */
    public SettledBetEvent processResultSettlePlus(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = rawGameSession.getAgentId();
        Integer vendorLineId = rawGameSession.getVendorLineId();
        Long vendorPlayerId = rawGameSession.getVendorPlayerId();
        String roundId = unsettledResultSettledData.getRoundId();
        String vendorBetId = unsettledResultSettledData.getVendorBetId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;

        // 1. Retrieve the rawUnsettledBet bet data
        RawUnsettledBet rawUnsettledBet = betHistoryService.getRawUnsettledBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 2. Generate rawSettledBet
        RawSettledBet rawSettledBet = this.newUnsettleResultSettledBet(rawUnsettledBet.getInternalTransactionId(), rawGameSession, unsettledResultSettledData, rawData);

        // 3. Combine rawUnsettledBet and rawSettledBet into final rawSettledBet data
        rawSettledBet = settledBetService.updateRawSettledBet(rawUnsettledBet, null, rawSettledBet);

        try {
            // 4. Prepare to send this transaction to operator with isFullBet is false
            WalletBalanceVo balanceVo = this.sendSettledWalletTransactionPlus(agentId, traceId, rawGameSession, rawSettledBet);
            SettledBetEvent settledBetEvent = new SettledBetEvent(rawSettledBet, balanceVo.getData().getBalance());

            // 5. Insert into couchbase settled table (and mariaDB if testing stub is disabled)
//            settledBetService.createSettledBet(rawSettledBet);
//            boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
//            if (stub == false) {
//                settledBetService.createSettleBetMariaDB(rawSettledBet);
//            }

            BetHistory betHistory = this.toBetHistory(rawSettledBet);
            kafkaService.produceBetHistory(betHistory);

            // 6. Create async thread to flush rawUnsettledBet in couchbase and redis
            EventDispatcherSystem.emitAsync(settledBetEvent);

            return settledBetEvent;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(rawSettledBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(rawSettledBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
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
     * @param traceId                    A unique Id for this request
     * @param rawGameSession                RawGameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the full bet details
     * @param rawData                    String body of entire vendor request params.
     * @return The player's current wallet
     */
    public SettledBetEvent processUnsettleResultSettle(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = rawGameSession.getAgentId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;

        // 1. Generate rawSettledBet
        RawSettledBet rawSettledBet = this.newUnsettleResultSettledBet(traceId, rawGameSession, unsettledResultSettledData, rawData);

        // 2. Insert into couchbase settled table (and mariaDB if testing stub is disabled)
//        settledBetService.createSettledBet(rawSettledBet);
//        boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
//        if (stub == false) {
//            settledBetService.createSettleBetMariaDB(rawSettledBet);
//        }

        BetHistory betHistory = this.toBetHistory(rawSettledBet);
        kafkaService.produceBetHistory(betHistory);

        try {
            // 3. Prepare to send this transaction to operator, with isFullBet as true
            WalletBalanceVo balanceVo = this.sendSettledWalletTransaction(agentId, traceId, rawGameSession, rawSettledBet, true);
            SettledBetEvent settledBetEvent = new SettledBetEvent(rawSettledBet, balanceVo.getData().getBalance());

            // TODO: if operator failed, we just resend and does not need to update any status on unsettled bet, so eventing is not needed anymore
            //EventDispatcherSystem.emitAsync(settledBetEvent);

            return settledBetEvent;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(rawSettledBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(rawSettledBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
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
     * @param traceId                    A unique Id for this request
     * @param rawGameSession                RawGameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the full bet details
     * @param rawData                    String body of entire vendor request params.
     * @return The player's current wallet
     */
    public SettledBetEvent processUnsettleResultSettlePlus(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = rawGameSession.getAgentId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;

        // 1. Generate rawSettledBet
        RawSettledBet rawSettledBet = this.newUnsettleResultSettledBet(traceId, rawGameSession, unsettledResultSettledData, rawData);

        try {
            // 2. Prepare to send this transaction to operator, with isFullBet as true
            WalletBalanceVo balanceVo = this.sendSettledWalletTransactionPlus(agentId, traceId, rawGameSession, rawSettledBet);
            SettledBetEvent settledBetEvent = new SettledBetEvent(rawSettledBet, balanceVo.getData().getBalance());

            // 3. Insert into couchbase settled table (and mariaDB if testing stub is disabled)
//            settledBetService.createSettledBet(rawSettledBet);
//            boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
//            if (stub == false) {
//                settledBetService.createSettleBetMariaDB(rawSettledBet);
//            }

            BetHistory betHistory = this.toBetHistory(rawSettledBet);
            kafkaService.produceBetHistory(betHistory);

            return settledBetEvent;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(rawSettledBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            settledBetOperatorFailEvent = new SettledBetOperatorFailEvent(rawSettledBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
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
     * @param rawGameSession RawGameSession object containing information of the vendor, game, player
     * @param winData     SettledData object containing information of the bet result
     * @param rawData     Raw data sent by vendor containing information of the bet result
     * @return BetResultEvent An event object containing Bet and Bet Result information as well as the last balance
     * that can be used for further processing, if required
     * @throws BetNotFoundException                    If no bet record is found
     * @throws DuplicateExternalTransactionIdException If vendor's transaction Id is found
     */
    public BetResultEvent processWin(String traceId, RawGameSession rawGameSession, WinData winData, String rawData) throws
            BetNotFoundException, DuplicateExternalTransactionIdException, InvalidOperatorResponseException, BetResultNotFoundException {

        Integer agentId = rawGameSession.getAgentId();
        Integer vendorGameId = rawGameSession.getVendorGameId();
        Long vendorPlayerId = rawGameSession.getVendorPlayerId();
        String roundId = winData.getRoundId();

        // 1. Retrieve the bet transaction
        BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(roundId, vendorGameId, vendorPlayerId);

        WalletWinDto walletWinDto = this.newWalletWinDto(traceId, rawGameSession, winData, betHistory.getId());

        BetResultLog betResultLog = this.newBetResultLog(traceId, rawGameSession, winData, betHistory, walletWinDto, rawData);
        BetResultEvent betResultEvent = null;
        Boolean requiredCallOperator = true;

        try {
            betResultLog = winData.prepareData(betHistory, betResultLog);
            betResultLogService.create(betResultLog);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {

            Integer getOperatorStatus = betHistoryService.getBetHistoryByExternalTransaction(betResultLog.getExternalTransactionId(), betResultLog.getRoundId(),
                    betResultLog.getVendorLineId()).getOperatorStatus();

            if (betResultLog.getOperatorStatus() == 1) {
                betResultEvent = new BetResultEvent(betHistory, betResultLog, BigDecimal.ZERO);
                requiredCallOperator = false;
            } else {
                betResultLog.setOperatorStatus(getOperatorStatus);
            }
        }

        //IF the bet ID is duplicated and not error, will return as 0 to vendor
        if (requiredCallOperator) {
            // TODO: To discuss if Agent is disable, should system ignore callback and just insert to bet_result_log
            try {
                AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
                WalletBalanceVo balanceVo = walletWinAction.call(agentApiCredential, walletWinDto);
                betResultEvent = new BetResultEvent(betHistory, betResultLog, balanceVo.getData().getBalance());

            } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
                betResultEvent = new BetResultEvent(betHistory, betResultLog, BigDecimal.ZERO);
                //Update bet_result_log operator status to agent is disable
                BetResultOperatorFailEvent betResultOperatorFailEvent =
                        new BetResultOperatorFailEvent(betResultLog, -1);
                EventDispatcherSystem.emitAsync(betResultOperatorFailEvent);

            } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
                //Update bet_result_log operator status based on exception
                BetResultOperatorFailEvent betResultOperatorFailEvent =
                        new BetResultOperatorFailEvent(betResultLog, invalidOperatorResponseException.getOperatorStatus());
                EventDispatcherSystem.emitAsync(betResultOperatorFailEvent);
                throw invalidOperatorResponseException;
            }

            EventDispatcherSystem.emitAsync(betResultEvent);
        }

        return betResultEvent;

    }


    /**
     * To process the result of a bet by sending the bet result data to Operator so that the Operator can update
     * the player's balance.
     *
     * @param traceId                    A unique Id for this request
     * @param rawGameSession                RawGameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet result
     * @param rawData                    Raw data sent by vendor containing information of the bet result
     * @return ResultBetEvent An event object containing Bet and Bet Result information as well as the last balance
     * that can be used for further processing, if required
     * @throws BetNotFoundException            If no bet record is found
     * @throws CouchbaseDataIntegrityException If anything wrong data inser into couchbase Id is found
     */
    public ResultBetEvent processResultBet(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            BetNotFoundException, InvalidOperatorResponseException, CouchbaseDataIntegrityException, InvalidAgentApiCredentialException {

        Integer agentId = rawGameSession.getAgentId();
        Integer vendorLineId = rawGameSession.getVendorLineId();
        Long vendorPlayerId = rawGameSession.getVendorPlayerId();
        String roundId = unsettledResultSettledData.getRoundId();
        String vendorBetId = unsettledResultSettledData.getVendorBetId();
        ResultBetOperatorFailEvent resultBetOperatorFailEvent = null;

        // 1. Retrieve the bet transaction
        RawUnsettledBet rawUnsettledBet = betHistoryService.getRawUnsettledBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 2. Generate wallet result dto
        WalletWinDto walletWinDto = this.newWalletResultDto(traceId, rawGameSession, unsettledResultSettledData, rawUnsettledBet.getId(), null, rawUnsettledBet.getResultTime());

        // 3. Generate raw result bet
        RawResultBet rawResultBet = this.newResultBet(rawGameSession, unsettledResultSettledData, rawData, rawUnsettledBet);

        // 4. Insert into couchbase result_bet table
        betResultLogService.createResultBet(rawResultBet);

        try {
            // 5. Prepare to send this transaction to operator as win
            AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
            WalletBalanceVo balanceVo = walletWinAction.call(agentApiCredential, walletWinDto);

            //TODO: refine proper handle for result bet event
            ResultBetEvent resultBetEvent = new ResultBetEvent(rawResultBet, balanceVo.getData().getBalance());
            //EventDispatcherSystem.emitAsync(resultBetEvent);
            return resultBetEvent;

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            //TODO: To discuss if Agent is disable, should we just remove the session of this player and return vendor with invalid bet request?
            resultBetOperatorFailEvent = new ResultBetOperatorFailEvent(rawResultBet, ResponseCodes.Status.SC_USER_DISABLED.code);
            throw invalidAgentApiCredentialException;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //TODO: if operator responses failed message, we should just move on and expect vendor resend?
            resultBetOperatorFailEvent = new ResultBetOperatorFailEvent(rawResultBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } finally {
            boolean isOperatorFailed = resultBetOperatorFailEvent != null;
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
     * @param traceId                    A unique Id for this request
     * @param rawGameSession                RawGameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet result
     * @param rawData                    Raw data sent by vendor containing information of the bet result
     * @return ResultBetEvent An event object containing Bet and Bet Result information as well as the last balance
     * that can be used for further processing, if required
     * @throws BetNotFoundException            If no bet record is found
     * @throws CouchbaseDataIntegrityException If anything wrong data inser into couchbase Id is found
     */
    public ResultBetEvent processResultBetPlus(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            BetNotFoundException, InvalidOperatorResponseException, CouchbaseDataIntegrityException, InvalidAgentApiCredentialException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = rawGameSession.getAgentId();
        Integer vendorLineId = rawGameSession.getVendorLineId();
        Long vendorPlayerId = rawGameSession.getVendorPlayerId();
        String roundId = unsettledResultSettledData.getRoundId();
        String vendorBetId = unsettledResultSettledData.getVendorBetId();
        ResultBetOperatorFailEvent resultBetOperatorFailEvent = null;

        // 1. Retrieve the bet transaction
        RawUnsettledBet rawUnsettledBet = betHistoryService.getRawUnsettledBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 2. Generate raw result bet
        RawResultBet rawResultBet = this.newResultBet(rawGameSession, unsettledResultSettledData, rawData, rawUnsettledBet);

        // 3. Combine unsettled bet, result bet
        RawSettledBet rawSettledBet = settledBetService.updateRawResultBet(rawUnsettledBet, rawResultBet);

        try {
            // 4. Prepare to send this transaction to operator as win
            WalletBalanceVo balanceVo = this.sendSettledWalletTransactionPlus(agentId, traceId, rawGameSession, rawSettledBet);

            //TODO: refine proper handle for result bet event
            ResultBetEvent resultBetEvent = new ResultBetEvent(rawResultBet, balanceVo.getData().getBalance());
            //EventDispatcherSystem.emitAsync(resultBetEvent);

            // 5. Insert into couchbase result_bet table
            betResultLogService.createResultBet(rawResultBet);

            return resultBetEvent;

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            //TODO: To discuss if Agent is disable, should we just remove the session of this player and return vendor with invalid bet request?
            resultBetOperatorFailEvent = new ResultBetOperatorFailEvent(rawResultBet, ResponseCodes.Status.SC_USER_DISABLED.code);
            throw invalidAgentApiCredentialException;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //TODO: if operator responses failed message, we should just move on and expect vendor resend?
            resultBetOperatorFailEvent = new ResultBetOperatorFailEvent(rawResultBet, invalidOperatorResponseException.getOperatorStatus());
            throw invalidOperatorResponseException;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            resultBetOperatorFailEvent = new ResultBetOperatorFailEvent(rawResultBet, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            throw insufficientBalanceException;

        } finally {
            boolean isOperatorFailed = resultBetOperatorFailEvent != null;
            if (isOperatorFailed) {
                // TODO: if operator failed, we just resend and does not need to update any status on unsettled bet, so eventing is not needed anymore
                //EventDispatcherSystem.emitAsync(unsettledBetOperatorFailEvent);
            }
        }
    }

    /**
     * To process the reversal of a bet by sending the refund instruction to Operator so that the Operator can perform
     * a reversal and return the updated balance of the player.
     *
     * @param traceId               A unique Id for this request
     * @param externalTransactionId Vendor's bet transaction Id of a previous bet record
     * @param rawGameSession           RawGameSession object containing information of the vendor, game, player
     * @param rawData               Raw data sent by vendor containing information of the Refund
     * @return BetRefundEvent An event object containing Bet and Refund information to be used for further processing, if required
     * @throws BetNotFoundException    If no bet record is found
     * @throws RecordNotFoundException Generic exception for orphan records
     */
    public BetRefundEvent processRefund(String traceId, String externalTransactionId, RawGameSession rawGameSession, String rawData) throws
            BetNotFoundException, RecordNotFoundException, InvalidAgentApiCredentialException, DuplicateExternalTransactionIdException, InvalidOperatorResponseException {

        Integer agentId = rawGameSession.getAgentId();
        Integer vendorId = rawGameSession.getVendorId();
        Long currentTimestamp = System.currentTimeMillis();
        Long vendorPlayerId = rawGameSession.getVendorPlayerId();

        // 1. Retrieve the bet transaction
        BetHistory betHistory = betHistoryService.getBetTransactionByVendorTransactionIdPlayerId(externalTransactionId, vendorId, vendorPlayerId);

        WalletRefundDto walletRefundDto = this.newWalletRefundDto(traceId, rawGameSession, currentTimestamp, betHistory);

        BetRefundLog betRefundLog = this.newBetRefundLog(betHistory, externalTransactionId, currentTimestamp, rawData);

        BetRefundEvent betRefundEvent = null;
        Boolean requiredCallOperator = true;
        try {
            betRefundLogService.create(betRefundLog);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {

            System.err.println(externalTransactionId);
            System.err.println(betHistory.getRoundId());
            System.err.println(rawGameSession.getVendorLineId());
            BetRefundLog currentBetRefundLog = betRefundLogService.findByExternalTransactionIdAndRoundIdAndVendorLineId(
                    externalTransactionId, betHistory.getRoundId(), rawGameSession.getVendorLineId());


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

    private WalletBetDto newWalletBetDto(String traceId, RawGameSession rawGameSession, BetData betData) {
        WalletBetDto walletBetDto = new WalletBetDto();
        walletBetDto.setTraceId(traceId);
        walletBetDto.setTransactionId(traceId);
        walletBetDto.setUsername(rawGameSession.getAgentPlayerUsername());
        walletBetDto.setCurrency(rawGameSession.getCurrencyCode());
        walletBetDto.setToken(rawGameSession.getToken());
        walletBetDto.setExternalTransactionId(betData.getExternalTransactionId());
        walletBetDto.setAmount(betData.getAmount());
        walletBetDto.setGameCode(rawGameSession.getGameCode());
        walletBetDto.setRoundId(betData.getRoundId());
        walletBetDto.setTimestamp(betData.getTimestamp());

        return walletBetDto;
    }

    private WalletBetDto newWalletUnsettledBetDto(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData) {
        WalletBetDto walletBetDto = new WalletBetDto();
        walletBetDto.setTraceId(traceId);
        walletBetDto.setTransactionId(traceId);
        walletBetDto.setUsername(rawGameSession.getAgentPlayerUsername());
        walletBetDto.setCurrency(rawGameSession.getCurrencyCode());
        walletBetDto.setToken(rawGameSession.getToken());
        walletBetDto.setExternalTransactionId(unsettledResultSettledData.getExternalTransactionId());
        walletBetDto.setAmount(new BigDecimal(unsettledResultSettledData.getBetAmount().stripTrailingZeros().toPlainString()));
        walletBetDto.setGameCode(rawGameSession.getGameCode());
        walletBetDto.setRoundId(unsettledResultSettledData.getRoundId());
        walletBetDto.setTimestamp(unsettledResultSettledData.getVendorBetTime());

        return walletBetDto;
    }

    private BetHistory newBetHistory(WalletBetDto walletBetDto, RawGameSession rawGameSession, String rawData) {
        BetHistory betHistory = new BetHistory();
        betHistory.setId(walletBetDto.getTraceId());
        betHistory.setExternalTransactionId(walletBetDto.getExternalTransactionId());
        betHistory.setRoundId(walletBetDto.getRoundId());
        betHistory.setVendorGameId(rawGameSession.getVendorGameId());
        betHistory.setVendorPlayerId(rawGameSession.getVendorPlayerId());
        betHistory.setVendorId(rawGameSession.getVendorId());
        betHistory.setAgentPlayerId(rawGameSession.getAgentPlayerId());
        betHistory.setAgentId(rawGameSession.getAgentId());
        betHistory.setVendorLineId(rawGameSession.getVendorLineId());
        betHistory.setGameCategoryId(rawGameSession.getGameCategoryId());
        betHistory.setCurrencyId(rawGameSession.getCurrencyId());
        betHistory.setBetAmount(walletBetDto.getAmount());
        betHistory.setGameSessionToken(rawGameSession.getToken());
        betHistory.setRawData(rawData);
        betHistory.setVendorBetTime(walletBetDto.getTimestamp());
        betHistory.setGameSessionToken(rawGameSession.getToken());
        betHistory.setOperatorStatus(1);

        return betHistory;
    }

    private RawUnsettledBet newUnsettledBet(RawGameSession rawGameSession, String rawData,
                                            UnsettledResultSettledData unsettledResultSettledData, String traceId) {

        RawUnsettledBet rawUnsettledBet = new RawUnsettledBet();
        String md5RawData = DigestUtils.md5Hex(rawData);

        rawUnsettledBet.setId(unsettledResultSettledData.getVendorBetId() + '_' + unsettledResultSettledData.getRoundId() + '_' + rawGameSession.getVendorLineId() + '_' + rawGameSession.getVendorPlayerId());
        rawUnsettledBet.setInternalTransactionId(traceId);
        rawUnsettledBet.setExternalTransactionId(unsettledResultSettledData.getExternalTransactionId());
        rawUnsettledBet.setRoundId(unsettledResultSettledData.getRoundId());
        rawUnsettledBet.setVendorGameId(rawGameSession.getVendorGameId());
        rawUnsettledBet.setVendorPlayerId(rawGameSession.getVendorPlayerId());
        rawUnsettledBet.setVendorId(rawGameSession.getVendorId());
        rawUnsettledBet.setAgentPlayerId(rawGameSession.getAgentPlayerId());
        rawUnsettledBet.setAgentId(rawGameSession.getAgentId());
        rawUnsettledBet.setVendorLineId(rawGameSession.getVendorLineId());
        rawUnsettledBet.setGameCategoryId(rawGameSession.getGameCategoryId());
        rawUnsettledBet.setCurrencyId(rawGameSession.getCurrencyId());
        rawUnsettledBet.setBetAmount(unsettledResultSettledData.getBetAmount());
        rawUnsettledBet.setGameSessionToken(rawGameSession.getToken());
        rawUnsettledBet.setResultType(unsettledResultSettledData.getResultType().code);
        rawUnsettledBet.setVendorBetTime(unsettledResultSettledData.getVendorBetTime());
        rawUnsettledBet.setGameSessionToken(rawGameSession.getToken());
        rawUnsettledBet.setOperatorStatus(1);
        rawUnsettledBet.setMd5RawSettledResult(md5RawData);
        rawUnsettledBet.setWinAmount(unsettledResultSettledData.getWinAmount());
        rawUnsettledBet.setWinLoss(unsettledResultSettledData.getWinLoss());
        rawUnsettledBet.setEffectiveTurnover(unsettledResultSettledData.getEffectiveTurnover());
        rawUnsettledBet.setRefundAmount(unsettledResultSettledData.getRefundAmount());
        rawUnsettledBet.setVendorSettleTime(unsettledResultSettledData.getVendorSettleTime());
        rawUnsettledBet.setResultTime(unsettledResultSettledData.getResultTime());
        rawUnsettledBet.setVendorBetId(unsettledResultSettledData.getVendorBetId());
        rawUnsettledBet.setJackpotAmount(unsettledResultSettledData.getJackpotAmount());
        rawUnsettledBet.setIsFreespin(unsettledResultSettledData.getIsFreespin());
        rawUnsettledBet.setStatus(unsettledResultSettledData.getBetStatus().code);

        return rawUnsettledBet;
    }

    private WalletWinDto newWalletWinDto(String traceId, RawGameSession rawGameSession, WinData winData, String referenceTransactionId) {
        WalletWinDto walletWinDto = new WalletWinDto();
        walletWinDto.setTraceId(traceId);
        walletWinDto.setTransactionId(traceId);
        walletWinDto.setUsername(rawGameSession.getAgentPlayerUsername());
        walletWinDto.setCurrency(rawGameSession.getCurrencyCode());
        walletWinDto.setToken(rawGameSession.getToken());
        walletWinDto.setExternalTransactionId(winData.getExternalTransactionId());
        walletWinDto.setReferenceTransactionId(referenceTransactionId);
        walletWinDto.setAmount(winData.getAmount());
        walletWinDto.setGameCode(rawGameSession.getGameCode());
        walletWinDto.setRoundId(winData.getRoundId());
        walletWinDto.setWinType(winData.getWinType());
        walletWinDto.setTimestamp(winData.getTimestamp());
        return walletWinDto;
    }

    private WalletWinDto newWalletResultDto(String traceId, RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData,
                                            String referenceTransactionId, BigDecimal transferAmount, Long timestamp) {

        WalletWinDto walletWinDto = new WalletWinDto();
        BigDecimal winAmount = (unsettledResultSettledData.getWinAmount() == null) ? BigDecimal.valueOf(0) : unsettledResultSettledData.getWinAmount();

        walletWinDto.setTraceId(traceId);
        walletWinDto.setTransactionId(traceId);
        walletWinDto.setUsername(rawGameSession.getAgentPlayerUsername());
        walletWinDto.setCurrency(rawGameSession.getCurrencyCode());
        walletWinDto.setToken(rawGameSession.getToken());
        walletWinDto.setExternalTransactionId(unsettledResultSettledData.getExternalTransactionId());
        walletWinDto.setReferenceTransactionId(referenceTransactionId);
        walletWinDto.setAmount((transferAmount == null) ? unsettledResultSettledData.getWinAmount() : transferAmount);
        walletWinDto.setGameCode(rawGameSession.getGameCode());
        walletWinDto.setRoundId(unsettledResultSettledData.getRoundId());
        walletWinDto.setWinType((winAmount.compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE);
        walletWinDto.setTimestamp(timestamp);

        return walletWinDto;
    }

    private WalletWinDto newWalletWinDtoForFullBetDto(String traceId, RawGameSession rawGameSession, RawSettledBet rawSettledBet, BigDecimal transferAmount) {

        BigDecimal winAmount = (rawSettledBet.getWinAmount() == null) ? BigDecimal.valueOf(0) : rawSettledBet.getWinAmount();

        WalletWinDto walletWinDto = new WalletWinDto();
        walletWinDto.setTraceId(traceId);
        walletWinDto.setTransactionId(traceId);
        walletWinDto.setUsername(rawGameSession.getAgentPlayerUsername());
        walletWinDto.setCurrency(rawGameSession.getCurrencyCode());
        walletWinDto.setToken(rawGameSession.getToken());
        walletWinDto.setExternalTransactionId(rawSettledBet.getExternalTransactionId());
        walletWinDto.setReferenceTransactionId(rawSettledBet.getInternalTransactionId());
        walletWinDto.setAmount(transferAmount);
        walletWinDto.setGameCode(rawGameSession.getGameCode());
        walletWinDto.setRoundId(rawSettledBet.getRoundId());
        walletWinDto.setWinType((winAmount.compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE);
        walletWinDto.setTimestamp(rawSettledBet.getVendorBetTime());
        return walletWinDto;
    }

    private WalletBetResultDto newWalletBetResultDtoForFullBetDto(String traceId, RawGameSession rawGameSession, RawSettledBet rawSettledBet) {

        WalletBetResultDto walletBetResultDto = new WalletBetResultDto();
        walletBetResultDto.setTraceId(traceId);
        walletBetResultDto.setUsername(rawGameSession.getAgentPlayerUsername());
        walletBetResultDto.setTransactionId(rawSettledBet.getInternalTransactionId());
        walletBetResultDto.setExternalTransactionId(rawSettledBet.getVendorBetId());
        walletBetResultDto.setExternalRoundId(rawSettledBet.getRoundId());
        walletBetResultDto.setBetAmount(new BigDecimal(rawSettledBet.getBetAmount().stripTrailingZeros().toPlainString()));
        walletBetResultDto.setWinAmount(new BigDecimal(rawSettledBet.getWinAmount().stripTrailingZeros().toPlainString()));
        walletBetResultDto.setEffectiveTurnover(new BigDecimal(rawSettledBet.getEffectiveTurnover().stripTrailingZeros().toPlainString()));
        walletBetResultDto.setJackpotAmount(new BigDecimal(rawSettledBet.getJackpotAmount().stripTrailingZeros().toPlainString()));
        walletBetResultDto.setWinLoss(new BigDecimal(rawSettledBet.getWinLoss().stripTrailingZeros().toPlainString()));
        walletBetResultDto.setResultType(rawSettledBet.getResultType());
        walletBetResultDto.setIsFreespin(rawSettledBet.getIsFreespin());
        walletBetResultDto.setIsEndRound((rawSettledBet.getStatus() == BetStatus.UNSETTLED.code)?0:1);
        walletBetResultDto.setCurrency(rawGameSession.getCurrencyCode());
        walletBetResultDto.setToken(rawGameSession.getToken());
        walletBetResultDto.setGameCode(rawGameSession.getGameCode());
        walletBetResultDto.setBetTime(rawSettledBet.getVendorBetTime());
        walletBetResultDto.setSettledTime(rawSettledBet.getVendorSettleTime());

        return walletBetResultDto;
    }

    private WalletBetDto newWalletBetForFullBetDto(String traceId, RawGameSession rawGameSession, RawSettledBet rawSettledBet) {
        WalletBetDto walletBetDto = new WalletBetDto();
        walletBetDto.setTraceId(traceId);
        walletBetDto.setTransactionId(traceId);
        walletBetDto.setUsername(rawGameSession.getAgentPlayerUsername());
        walletBetDto.setCurrency(rawGameSession.getCurrencyCode());
        walletBetDto.setToken(rawGameSession.getToken());
        walletBetDto.setExternalTransactionId(rawSettledBet.getExternalTransactionId());
        walletBetDto.setAmount(rawSettledBet.getWinLoss().abs());
        walletBetDto.setGameCode(rawGameSession.getGameCode());
        walletBetDto.setRoundId(rawSettledBet.getRoundId());
        walletBetDto.setTimestamp(rawSettledBet.getVendorBetTime());

        return walletBetDto;
    }

    private BetResultLog newBetResultLog(String traceId, RawGameSession rawGameSession, WinData winData, BetHistory betHistory, WalletWinDto walletWinDto, String rawData) {
        BetResultLog betResultLog = new BetResultLog();

        betResultLog.setId(traceId);
        betResultLog.setBetHistoryId(walletWinDto.getReferenceTransactionId());
        betResultLog.setExternalTransactionId(walletWinDto.getExternalTransactionId());
        betResultLog.setRoundId(betHistory.getRoundId());
        betResultLog.setVendorGameId(rawGameSession.getVendorGameId());
        betResultLog.setVendorPlayerId(rawGameSession.getVendorPlayerId());
        betResultLog.setAgentPlayerId(rawGameSession.getAgentPlayerId());
        betResultLog.setAgentId(rawGameSession.getAgentId());
        betResultLog.setVendorLineId(rawGameSession.getVendorLineId());
        betResultLog.setCurrencyId(rawGameSession.getCurrencyId());
        betResultLog.setOperatorStatus(1);
        betResultLog.setWinAmount(walletWinDto.getAmount());
        betResultLog.setEffectiveTurnover(winData.getEffectiveTurnover());
        betResultLog.setResultType(winData.getWinType().code);
        //TODO remove the balance column from bet_result_log table
        betResultLog.setBalance(BigDecimal.ZERO);
        betResultLog.setRawData(rawData);
        betResultLog.setVendorTime(walletWinDto.getTimestamp());

        return betResultLog;
    }

    private RawResultBet newResultBet(RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData,
                                      String rawData, RawUnsettledBet rawUnsettledBet) {

        RawResultBet rawResultBet = new RawResultBet();
        String md5RawData = DigestUtils.md5Hex(rawData);
        BigDecimal winLoss = unsettledResultSettledData.getWinAmount().subtract(rawUnsettledBet.getBetAmount());

        rawResultBet.setId(unsettledResultSettledData.getVendorBetId() + '_' + unsettledResultSettledData.getRoundId() + '_' + rawGameSession.getVendorLineId() + '_' + rawGameSession.getVendorPlayerId());
        rawResultBet.setInternalTransactionId(rawUnsettledBet.getInternalTransactionId());
        rawResultBet.setExternalTransactionId(unsettledResultSettledData.getExternalTransactionId());
        rawResultBet.setRoundId(unsettledResultSettledData.getRoundId());
        rawResultBet.setVendorGameId(rawGameSession.getVendorGameId());
        rawResultBet.setVendorPlayerId(rawGameSession.getVendorPlayerId());
        rawResultBet.setAgentPlayerId(rawGameSession.getAgentPlayerId());
        rawResultBet.setAgentId(rawGameSession.getAgentId());
        rawResultBet.setVendorLineId(rawGameSession.getVendorLineId());
        rawResultBet.setCurrencyId(rawGameSession.getCurrencyId());
        rawResultBet.setOperatorStatus(1);
        rawResultBet.setWinAmount(unsettledResultSettledData.getWinAmount());
        rawResultBet.setEffectiveTurnover(rawUnsettledBet.getBetAmount());
        rawResultBet.setWinLoss(winLoss);
        rawResultBet.setResultType(unsettledResultSettledData.getResultType().code);
        rawResultBet.setMd5RawSettledResult(md5RawData);
        rawResultBet.setResultTime(unsettledResultSettledData.getResultTime());
        rawResultBet.setVendorSettleTime(unsettledResultSettledData.getVendorSettleTime());
        rawResultBet.setRefundAmount(unsettledResultSettledData.getRefundAmount());
        rawResultBet.setBetAmount(rawUnsettledBet.getBetAmount());
        rawResultBet.setJackpotAmount(unsettledResultSettledData.getJackpotAmount());
        rawResultBet.setVendorBetId(unsettledResultSettledData.getVendorBetId());
        rawResultBet.setIsFreespin(unsettledResultSettledData.getIsFreespin());
        rawResultBet.setStatus(unsettledResultSettledData.getBetStatus().code);

        return rawResultBet;
    }

    private RawSettledBet newSettledBet(RawGameSession rawGameSession, UnsettledResultSettledData unsettledResultSettledData)
            throws MergedBetDataIntegrityException {

        try {
            RawSettledBet rawSettledBet = new RawSettledBet();
            BeanUtils.copyProperties(rawSettledBet, unsettledResultSettledData);

            rawSettledBet.setId(unsettledResultSettledData.getVendorBetId() + '_' + unsettledResultSettledData.getRoundId() + '_' + rawGameSession.getVendorLineId() + '_' + rawGameSession.getVendorPlayerId());
            rawSettledBet.setVendorGameId(rawGameSession.getVendorGameId());
            rawSettledBet.setVendorPlayerId(rawGameSession.getVendorPlayerId());
            rawSettledBet.setAgentPlayerId(rawGameSession.getAgentPlayerId());
            rawSettledBet.setAgentId(rawGameSession.getAgentId());
            rawSettledBet.setVendorLineId(rawGameSession.getVendorLineId());
            rawSettledBet.setCurrencyId(rawGameSession.getCurrencyId());
            rawSettledBet.setStatus(BetStatus.SETTLED.code);

            return rawSettledBet;

        } catch (IllegalAccessException illegalAccessException) {
            throw new MergedBetDataIntegrityException("getValueFromObject invalid : " + illegalAccessException.getMessage());

        } catch (InvocationTargetException invocationTargetException) {
            throw new MergedBetDataIntegrityException("copyProperties invalid : " + invocationTargetException.getMessage());
        }

    }

    private RawSettledBet newUnsettleResultSettledBet(String traceId, RawGameSession rawGameSession,
                                                      UnsettledResultSettledData unsettledResultSettledData, String rawData) {

        RawSettledBet rawSettledBet = new RawSettledBet();
        String md5RawData = DigestUtils.md5Hex(rawData);

        rawSettledBet.setId(unsettledResultSettledData.getVendorBetId() + '_' + unsettledResultSettledData.getRoundId() + '_' + rawGameSession.getVendorLineId() + '_' + rawGameSession.getVendorPlayerId());
        rawSettledBet.setInternalTransactionId(traceId);
        rawSettledBet.setExternalTransactionId(unsettledResultSettledData.getExternalTransactionId());
        rawSettledBet.setRoundId(unsettledResultSettledData.getRoundId());
        rawSettledBet.setVendorGameId(rawGameSession.getVendorGameId());
        rawSettledBet.setVendorPlayerId(rawGameSession.getVendorPlayerId());
        rawSettledBet.setVendorId(rawGameSession.getVendorId());
        rawSettledBet.setVendorLineId(rawGameSession.getVendorLineId());
        rawSettledBet.setAgentPlayerId(rawGameSession.getAgentPlayerId());
        rawSettledBet.setAgentId(rawGameSession.getAgentId());
        rawSettledBet.setGameCategoryId(rawGameSession.getGameCategoryId());
        rawSettledBet.setCurrencyId(rawGameSession.getCurrencyId());
        rawSettledBet.setBetAmount(unsettledResultSettledData.getBetAmount());
        rawSettledBet.setWinAmount(unsettledResultSettledData.getWinAmount());
        rawSettledBet.setWinLoss(unsettledResultSettledData.getWinLoss());
        rawSettledBet.setEffectiveTurnover(unsettledResultSettledData.getEffectiveTurnover());
        rawSettledBet.setRefundAmount(unsettledResultSettledData.getRefundAmount());
        rawSettledBet.setResultType(unsettledResultSettledData.getResultType().code);
        rawSettledBet.setMd5RawSettledResult(md5RawData);
        rawSettledBet.setResettleNum(0);
        rawSettledBet.setGameSessionToken(rawGameSession.getToken());
        rawSettledBet.setVendorBetTime(unsettledResultSettledData.getVendorBetTime());
        rawSettledBet.setVendorSettleTime(unsettledResultSettledData.getVendorSettleTime());
        rawSettledBet.setResultTime(unsettledResultSettledData.getResultTime());
        rawSettledBet.setVendorBetId(unsettledResultSettledData.getVendorBetId());
        rawSettledBet.setIsFreespin(unsettledResultSettledData.getIsFreespin());
        rawSettledBet.setJackpotAmount(unsettledResultSettledData.getJackpotAmount());
        rawSettledBet.setStatus(BetStatus.SETTLED.code);

        return rawSettledBet;
    }

    private WalletRefundDto newWalletRefundDto(String traceId, RawGameSession rawGameSession, Long currentTimestamp, BetHistory betHistory) {
        WalletRefundDto walletRefundDto = new WalletRefundDto();
        walletRefundDto.setTraceId(traceId);
        walletRefundDto.setTransactionId(traceId);
        walletRefundDto.setUsername(rawGameSession.getAgentPlayerUsername());
        walletRefundDto.setExternalTransactionId(betHistory.getExternalTransactionId());
        walletRefundDto.setReferenceTransactionId(betHistory.getId());
        walletRefundDto.setGameCode(rawGameSession.getGameCode());
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

    private WalletBalanceDto newWalletBalanceDto(String traceId, RawGameSession rawGameSession) {
        WalletBalanceDto walletBalanceDto = new WalletBalanceDto();
        walletBalanceDto.setTraceId(traceId);
        walletBalanceDto.setUsername(rawGameSession.getAgentPlayerUsername());
        walletBalanceDto.setCurrency(rawGameSession.getCurrencyCode());
        walletBalanceDto.setToken(rawGameSession.getToken());

        return walletBalanceDto;
    }

    private WalletBalanceVo sendSettledWalletTransaction(Integer agentId, String traceId, RawGameSession rawGameSession, RawSettledBet rawSettledBet, Boolean isFullBet)
            throws InvalidAgentApiCredentialException, InvalidOperatorResponseException, InsufficientBalanceException {

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        WalletBalanceVo balanceVo;

        if (isFullBet == true) {
            //if isFullBet = true, then we will compare with using winLoss to decide send as wallet/win or lose
            if ((rawSettledBet.getWinLoss().compareTo(BigDecimal.ZERO) >= 0)) {
                //if WinLoss >= 0 then we will send as win
                WalletWinDto walletWinDto = this.newWalletWinDtoForFullBetDto(traceId, rawGameSession, rawSettledBet, rawSettledBet.getWinLoss());
                balanceVo = walletWinAction.call(agentApiCredential, walletWinDto);

            } else {
                //else send as lose
                WalletBetDto walletBetDto = this.newWalletBetForFullBetDto(traceId, rawGameSession, rawSettledBet);
                balanceVo = walletBetAction.call(agentApiCredential, walletBetDto);
            }
        } else {
            //else isFullBet = false, then we will send as wallet/win with winAmount (because bet already deducted)
            WalletWinDto walletWinDto = this.newWalletWinDtoForFullBetDto(traceId, rawGameSession, rawSettledBet, rawSettledBet.getWinAmount());
            balanceVo = walletWinAction.call(agentApiCredential, walletWinDto);
        }

        return balanceVo;
    }

    //temporary updated version for sendSettledWalletTransaction function, avoid changes update to stg environment
    private WalletBalanceVo sendSettledWalletTransactionPlus(Integer agentId, String traceId, RawGameSession rawGameSession, RawSettledBet rawSettledBet)
            throws InvalidAgentApiCredentialException, InvalidOperatorResponseException, InsufficientBalanceException {

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        WalletBetResultDto walletBetResultDto = this.newWalletBetResultDtoForFullBetDto(traceId, rawGameSession, rawSettledBet);
        WalletBalanceVo balanceVo = walletBetResultAction.call(agentApiCredential, walletBetResultDto);

        return balanceVo;
    }

    private BetHistory toBetHistory(RawSettledBet rawSettledBet) throws MergedBetDataIntegrityException {

        try {
            BetHistory betHistory = new BetHistory();
            BeanUtils.copyProperties(betHistory, rawSettledBet);
            betHistory.setRawData(rawSettledBet.getMd5RawSettledResult());
            //TODO REMOVING OPERATORSTATUS
            betHistory.setOperatorStatus(1);
            betHistory.setId(rawSettledBet.getInternalTransactionId());
            betHistory.setCreateTime(Instant.now().toEpochMilli());

            return betHistory;

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new MergedBetDataIntegrityException("copyProperties invalid : " + e.getMessage());
        }
    }
}
