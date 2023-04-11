package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.*;
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
import com.nextgen.gameaggregator.util.ApiSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

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

    public BigDecimal getBalance(String traceId, GameSession gameSession) throws InvalidOperatorResponseException, InvalidAgentApiCredentialException {
        Integer agentId = gameSession.getAgentId();
        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String callbackUrl = agentApiCredential.getCallbackUrl();

        WalletBalanceDto walletBalanceDto = this.newWalletBalanceDto(traceId, gameSession);
        String signature = authenticationService.generateSignature(walletBalanceDto, agentApiCredential.getApiSecret());


        WalletBalanceVo balanceVo = walletBalanceAction.call(callbackUrl, signature, walletBalanceDto);
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
     * @param gameSession GameSession object containing information of the vendor, game, player
     * @param betData     BetData object containing information of the bet such as betAmount, game, betTime
     * @param rawData     Raw data sent by vendor containing information of the bet
     * @return The player's current wallet balance after deducting the bet amount
     */
    public BetEvent processBet(String traceId, GameSession gameSession, BetData betData, String rawData) throws
            InsufficientBalanceException, DuplicateExternalTransactionIdException,
            InvalidOperatorResponseException, InvalidAgentApiCredentialException {

        Integer agentId = gameSession.getAgentId();
        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);

        String callbackUrl = agentApiCredential.getCallbackUrl();

        WalletBetDto walletBetDto = this.newWalletBetDto(traceId, gameSession, betData);
        String signature = authenticationService.generateSignature(walletBetDto, agentApiCredential.getApiSecret());



        BetHistory betHistory = this.newBetHistory(walletBetDto, gameSession, rawData);
        betHistoryService.create(betHistory);
        BetOperatorFailEvent betOperatorFailEvent = null;

        try {
//            WalletBalanceVo balanceVo = walletBetAction.stub();
            WalletBalanceVo balanceVo = walletBetAction.call(callbackUrl, signature, walletBetDto);
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
     * @param gameSession                GameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @param rawData                    Raw data sent by vendor containing information of the bet
     * @return The player's current wallet balance after deducting the bet amount
     */
    public UnsettledBetEvent processUnsettledBet(String traceId, GameSession gameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            InsufficientBalanceException, CouchbaseDataIntegrityException, InvalidOperatorResponseException,
            InvalidAgentApiCredentialException {

        Integer agentId = gameSession.getAgentId();
        UnsettledBetOperatorFailEvent unsettledBetOperatorFailEvent = null;

        // 1. Generate walletBetDto
        WalletBetDto walletBetDto = this.newWalletUnsettledBetDto(traceId, gameSession, unsettledResultSettledData);

        // 2. Generate rawUnsettledBet
        RawUnsettledBet rawUnsettledBet = this.newUnsettledBet(walletBetDto, gameSession, rawData, unsettledResultSettledData);

        // 3. Insert into couchbase unsettled_bet table
        betHistoryService.createUnsettledBet(rawUnsettledBet);

        try {
            // 4. Prepare callback info
            AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
            String callbackUrl = agentApiCredential.getCallbackUrl();
            String signature = authenticationService.generateSignature(walletBetDto, agentApiCredential.getApiSecret());

            // 5. send this unsettled bet to operator
            if(agentId ==2){
                Gson gson = new Gson();
                String jsonPayload = gson.toJson(walletBetDto);
                String actualSignature = ApiSecurityUtils.getHmacSignature(jsonPayload, agentApiCredential.getApiSecret());

                log.error("CHECK-AGENT-SECRET :" + agentApiCredential.getApiSecret());
                log.error("CHECK-AGENT-PAYLOAD :" + jsonPayload);
                log.error("CHECK-AGENT-SIGNATURE :" + actualSignature);

            }

            WalletBalanceVo balanceVo = walletBetAction.call(callbackUrl, signature, walletBetDto);
            UnsettledBetEvent unsettledBetEvent = new UnsettledBetEvent(rawUnsettledBet, balanceVo.getData().getBalance());

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
     * @param gameSession                GameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @return The player's current wallet
     */
    public SettledBetEvent processSettledBet(String traceId, GameSession gameSession, UnsettledResultSettledData unsettledResultSettledData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException {

        Integer agentId = gameSession.getAgentId();
        Integer vendorLineId = gameSession.getVendorLineId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String roundId = unsettledResultSettledData.getRoundId();
        String vendorBetId = unsettledResultSettledData.getVendorBetId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;
        BigDecimal transferAmount = (unsettledResultSettledData.getWinLoss() == null) ? BigDecimal.valueOf(0) : unsettledResultSettledData.getWinLoss();

        // 1. Retrieve unsettled bet from couchbase
        RawUnsettledBet rawUnsettledBet = betHistoryService.getRawUnsettledBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 2. Retrieve result bet from couchbase
        RawResultBet rawResultBet = betResultLogService.getRawResultBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 3. Generate settled bet with end round bet data
        RawSettledBet rawSettledBet = this.newSettledBet(traceId, gameSession, unsettledResultSettledData);

        // 4. Combine unsettled bet, result bet and end round bet data into settle bet
        rawSettledBet = settledBetService.updateRawSettledBet(rawUnsettledBet, rawResultBet, rawSettledBet);

        // 5. Prepare wallet settled dto
        WalletWinDto walletWinDto = this.newWalletResultDto(traceId, gameSession, unsettledResultSettledData, rawUnsettledBet.getId(), transferAmount, rawSettledBet.getResultTime());

        // 6. Insert into couchbase settled_bet table and also mariadb
        settledBetService.createSettledBet(rawSettledBet);
        boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
        if (stub == false) {
            settledBetService.createSettleBetMariaDB(rawSettledBet);
        }

        try {
            // 7. Prepare to send this transaction to operator as win
            AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
            String callbackUrl = agentApiCredential.getCallbackUrl();
            String signature = authenticationService.generateSignature(walletWinDto, agentApiCredential.getApiSecret());
            WalletBalanceVo balanceVo = walletWinAction.call(callbackUrl, signature, walletWinDto);

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
     * @param gameSession                GameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet such as betAmount, game, betTime
     * @return entire SettledBetEvent in case vendor need more than balance from the process flow.
     */
    public SettledBetEvent processResultSettle(String traceId, GameSession gameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = gameSession.getAgentId();
        Integer vendorLineId = gameSession.getVendorLineId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String roundId = unsettledResultSettledData.getRoundId();
        String vendorBetId = unsettledResultSettledData.getVendorBetId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;

        // 1. Retrieve the rawUnsettledBet bet data
        RawUnsettledBet rawUnsettledBet = betHistoryService.getRawUnsettledBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 2. Generate rawSettledBet
        RawSettledBet rawSettledBet = this.newUnsettleResultSettledBet(rawUnsettledBet.getInternalTransactionId(), gameSession, unsettledResultSettledData, rawData);

        // 3. Combine rawUnsettledBet and rawSettledBet into final rawSettledBet data
        rawSettledBet = settledBetService.updateRawSettledBet(rawUnsettledBet, null, rawSettledBet);

        // 4. Insert into couchbase settled table (and mariaDB if testing stub is disabled)
        settledBetService.createSettledBet(rawSettledBet);
        boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
        if (stub == false) {
            settledBetService.createSettleBetMariaDB(rawSettledBet);
        }

        try {
            // 5. Prepare to send this transaction to operator with isFullBet is false
            WalletBalanceVo balanceVo = this.sendSettledWalletTransaction(agentId, traceId, gameSession, rawSettledBet, false);
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
     * To process the full bet (unsettle + result + end round) and getting the balance of player from operator
     *
     * @param traceId                    A unique Id for this request
     * @param gameSession                GameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the full bet details
     * @param rawData                    String body of entire vendor request params.
     * @return The player's current wallet
     */
    public SettledBetEvent processUnsettleResultSettle(String traceId, GameSession gameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = gameSession.getAgentId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;

        // 1. Generate rawSettledBet
        RawSettledBet rawSettledBet = this.newUnsettleResultSettledBet(traceId, gameSession, unsettledResultSettledData, rawData);

        // 2. Insert into couchbase settled table (and mariaDB if testing stub is disabled)
        settledBetService.createSettledBet(rawSettledBet);
        boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
        if (stub == false) {
            settledBetService.createSettleBetMariaDB(rawSettledBet);
        }

        try {
            // 3. Prepare to send this transaction to operator, with isFullBet as true
            WalletBalanceVo balanceVo = this.sendSettledWalletTransaction(agentId, traceId, gameSession, rawSettledBet, true);
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
     * @param gameSession                GameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the full bet details
     * @param rawData                    String body of entire vendor request params.
     * @return The player's current wallet
     */
    public SettledBetEvent processUnsettleResultSettlePlus(String traceId, GameSession gameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            CouchbaseDataIntegrityException, InvalidOperatorResponseException, InvalidAgentApiCredentialException,
            BetNotFoundException, MergedBetDataIntegrityException, InsufficientBalanceException {

        Integer agentId = gameSession.getAgentId();
        SettledBetOperatorFailEvent settledBetOperatorFailEvent = null;

        // 1. Generate rawSettledBet
        RawSettledBet rawSettledBet = this.newUnsettleResultSettledBet(traceId, gameSession, unsettledResultSettledData, rawData);

        // 2. Insert into couchbase settled table (and mariaDB if testing stub is disabled)
        settledBetService.createSettledBet(rawSettledBet);
        boolean stub = Boolean.parseBoolean(environment.getProperty("testing.stub"));
        if (stub == false) {
            settledBetService.createSettleBetMariaDB(rawSettledBet);
        }

        try {
            // 3. Prepare to send this transaction to operator, with isFullBet as true
            WalletBalanceVo balanceVo = this.sendSettledWalletTransactionPlus(agentId, traceId, gameSession, rawSettledBet, true);
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
     * To process the result of a bet by sending the bet result data to Operator so that the Operator can update
     * the player's balance.
     *
     * @param traceId     A unique Id for this request
     * @param gameSession GameSession object containing information of the vendor, game, player
     * @param winData     SettledData object containing information of the bet result
     * @param rawData     Raw data sent by vendor containing information of the bet result
     * @return BetResultEvent An event object containing Bet and Bet Result information as well as the last balance
     * that can be used for further processing, if required
     * @throws BetNotFoundException                    If no bet record is found
     * @throws DuplicateExternalTransactionIdException If vendor's transaction Id is found
     */
    public BetResultEvent processWin(String traceId, GameSession gameSession, WinData winData, String rawData) throws
            BetNotFoundException, DuplicateExternalTransactionIdException, InvalidOperatorResponseException, BetResultNotFoundException {

        Integer agentId = gameSession.getAgentId();
        Integer vendorGameId = gameSession.getVendorGameId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String roundId = winData.getRoundId();

        // 1. Retrieve the bet transaction
        BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(roundId, vendorGameId, vendorPlayerId);

        WalletWinDto walletWinDto = this.newWalletWinDto(traceId, gameSession, winData, betHistory.getId());

        BetResultLog betResultLog = this.newBetResultLog(traceId, gameSession, winData, betHistory, walletWinDto, rawData);
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
                String callbackUrl = agentApiCredential.getCallbackUrl();
                String signature = authenticationService.generateSignature(walletWinDto, agentApiCredential.getApiSecret());
                WalletBalanceVo balanceVo = walletWinAction.call(callbackUrl, signature, walletWinDto);
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
     * @param gameSession                GameSession object containing information of the vendor, game, player
     * @param unsettledResultSettledData UnsettledResultSettledData object containing information of the bet result
     * @param rawData                    Raw data sent by vendor containing information of the bet result
     * @return ResultBetEvent An event object containing Bet and Bet Result information as well as the last balance
     * that can be used for further processing, if required
     * @throws BetNotFoundException            If no bet record is found
     * @throws CouchbaseDataIntegrityException If anything wrong data inser into couchbase Id is found
     */
    public ResultBetEvent processResultBet(String traceId, GameSession gameSession, UnsettledResultSettledData unsettledResultSettledData, String rawData) throws
            BetNotFoundException, InvalidOperatorResponseException, CouchbaseDataIntegrityException, InvalidAgentApiCredentialException {

        Integer agentId = gameSession.getAgentId();
        Integer vendorLineId = gameSession.getVendorLineId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String roundId = unsettledResultSettledData.getRoundId();
        String vendorBetId = unsettledResultSettledData.getVendorBetId();
        ResultBetOperatorFailEvent resultBetOperatorFailEvent = null;

        // 1. Retrieve the bet transaction
        RawUnsettledBet rawUnsettledBet = betHistoryService.getRawUnsettledBetByRoundId(vendorBetId, roundId, vendorLineId, vendorPlayerId);

        // 2. Generate wallet result dto
        WalletWinDto walletWinDto = this.newWalletResultDto(traceId, gameSession, unsettledResultSettledData, rawUnsettledBet.getId(), null, rawUnsettledBet.getResultTime());

        // 3. Generate raw result bet
        RawResultBet rawResultBet = this.newResultBet(traceId, gameSession, unsettledResultSettledData, rawData, rawUnsettledBet);

        // 4. Insert into couchbase result_bet table
        betResultLogService.createResultBet(rawResultBet);

        try {
            // 5. Prepare to send this transaction to operator as win
            AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
            String callbackUrl = agentApiCredential.getCallbackUrl();
            String signature = authenticationService.generateSignature(walletWinDto, agentApiCredential.getApiSecret());
            WalletBalanceVo balanceVo = walletWinAction.call(callbackUrl, signature, walletWinDto);

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
     * To process the reversal of a bet by sending the refund instruction to Operator so that the Operator can perform
     * a reversal and return the updated balance of the player.
     *
     * @param traceId               A unique Id for this request
     * @param externalTransactionId Vendor's bet transaction Id of a previous bet record
     * @param gameSession           GameSession object containing information of the vendor, game, player
     * @param rawData               Raw data sent by vendor containing information of the Refund
     * @return BetRefundEvent An event object containing Bet and Refund information to be used for further processing, if required
     * @throws BetNotFoundException    If no bet record is found
     * @throws RecordNotFoundException Generic exception for orphan records
     */
    public BetRefundEvent processRefund(String traceId, String externalTransactionId, GameSession gameSession, String rawData) throws
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
                String callbackUrl = agentApiCredentialService.getAgentApiCredential(betHistory.getAgentId()).getCallbackUrl();
                String signature = authenticationService.generateSignature(walletRefundDto, agentApiCredential.getApiSecret());
                WalletBalanceVo balanceVo = walletRefundAction.call(callbackUrl, signature, walletRefundDto);

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

    private WalletBetDto newWalletBetDto(String traceId, GameSession gameSession, BetData betData) {
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

    private WalletBetDto newWalletUnsettledBetDto(String traceId, GameSession gameSession, UnsettledResultSettledData unsettledResultSettledData) {
        WalletBetDto walletBetDto = new WalletBetDto();
        walletBetDto.setTraceId(traceId);
        walletBetDto.setTransactionId(traceId);
        walletBetDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBetDto.setCurrency(gameSession.getCurrencyCode());
        walletBetDto.setToken(gameSession.getToken());
        walletBetDto.setExternalTransactionId(unsettledResultSettledData.getExternalTransactionId());
        walletBetDto.setAmount(unsettledResultSettledData.getBetAmount());
        walletBetDto.setGameCode(gameSession.getGameCode());
        walletBetDto.setRoundId(unsettledResultSettledData.getRoundId());
        walletBetDto.setTimestamp(unsettledResultSettledData.getVendorBetTime());

        return walletBetDto;
    }

    private BetHistory newBetHistory(WalletBetDto walletBetDto, GameSession gameSession, String rawData) {
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
        betHistory.setMasterAgentId(0); // TODO: populate with actual value
        betHistory.setHouseId(0);
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

    private RawUnsettledBet newUnsettledBet(WalletBetDto walletBetDto, GameSession gameSession, String rawData,
                                            UnsettledResultSettledData unsettledResultSettledData) {

        RawUnsettledBet rawUnsettledBet = new RawUnsettledBet();
        String md5RawData = DigestUtils.md5Hex(rawData);

        rawUnsettledBet.setId(unsettledResultSettledData.getVendorBetId() + '_' + walletBetDto.getRoundId() + '_' + gameSession.getVendorLineId() + '_' + gameSession.getVendorPlayerId());
        rawUnsettledBet.setInternalTransactionId(walletBetDto.getTraceId());
        rawUnsettledBet.setExternalTransactionId(walletBetDto.getExternalTransactionId());
        rawUnsettledBet.setRoundId(walletBetDto.getRoundId());
        rawUnsettledBet.setVendorGameId(gameSession.getVendorGameId());
        rawUnsettledBet.setVendorPlayerId(gameSession.getVendorPlayerId());
        rawUnsettledBet.setVendorId(gameSession.getVendorId());
        rawUnsettledBet.setAgentPlayerId(gameSession.getAgentPlayerId());
        rawUnsettledBet.setAgentId(gameSession.getAgentId());
        rawUnsettledBet.setVendorLineId(gameSession.getVendorLineId());
        rawUnsettledBet.setMasterAgentId(0); // TODO: populate with actual value
        rawUnsettledBet.setHouseId(0);
        rawUnsettledBet.setGameCategoryId(gameSession.getGameCategoryId());
        rawUnsettledBet.setCurrencyId(gameSession.getCurrencyId());
        rawUnsettledBet.setBetAmount(walletBetDto.getAmount());
        rawUnsettledBet.setGameSessionToken(gameSession.getToken());
        rawUnsettledBet.setResultType(unsettledResultSettledData.getResultType().code);
        rawUnsettledBet.setVendorBetTime(walletBetDto.getTimestamp());
        rawUnsettledBet.setGameSessionToken(gameSession.getToken());
        rawUnsettledBet.setOperatorStatus(1);
        rawUnsettledBet.setMd5RawSettledResult(md5RawData);
        rawUnsettledBet.setWinAmount(unsettledResultSettledData.getWinAmount());
        rawUnsettledBet.setWinLoss(unsettledResultSettledData.getWinLoss());
        rawUnsettledBet.setVendorWinLoss(unsettledResultSettledData.getVendorWinLoss());
        rawUnsettledBet.setEffectiveTurnover(unsettledResultSettledData.getEffectiveTurnover());
        rawUnsettledBet.setRefundAmount(unsettledResultSettledData.getRefundAmount());
        rawUnsettledBet.setVendorSettleTime(unsettledResultSettledData.getVendorSettleTime());
        rawUnsettledBet.setResultTime(unsettledResultSettledData.getResultTime());
        rawUnsettledBet.setVendorBetId(unsettledResultSettledData.getVendorBetId());
        rawUnsettledBet.setJackpotAmount(unsettledResultSettledData.getJackpotAmount());
        rawUnsettledBet.setIsCancelled(unsettledResultSettledData.getIsCancelled());
        rawUnsettledBet.setIsFreespin(unsettledResultSettledData.getIsFreespin());

        return rawUnsettledBet;
    }

    private WalletWinDto newWalletWinDto(String traceId, GameSession gameSession, WinData winData, String referenceTransactionId) {
        WalletWinDto walletWinDto = new WalletWinDto();
        walletWinDto.setTraceId(traceId);
        walletWinDto.setTransactionId(traceId);
        walletWinDto.setUsername(gameSession.getAgentPlayerUsername());
        walletWinDto.setCurrency(gameSession.getCurrencyCode());
        walletWinDto.setToken(gameSession.getToken());
        walletWinDto.setExternalTransactionId(winData.getExternalTransactionId());
        walletWinDto.setReferenceTransactionId(referenceTransactionId);
        walletWinDto.setAmount(winData.getAmount());
        walletWinDto.setGameCode(gameSession.getGameCode());
        walletWinDto.setRoundId(winData.getRoundId());
        walletWinDto.setWinType(winData.getWinType());
        walletWinDto.setTimestamp(winData.getTimestamp());
        return walletWinDto;
    }

    private WalletWinDto newWalletResultDto(String traceId, GameSession gameSession, UnsettledResultSettledData unsettledResultSettledData,
                                            String referenceTransactionId, BigDecimal transferAmount, Long timestamp) {

        WalletWinDto walletWinDto = new WalletWinDto();
        BigDecimal winAmount = (unsettledResultSettledData.getWinAmount() == null) ? BigDecimal.valueOf(0) : unsettledResultSettledData.getWinAmount();

        walletWinDto.setTraceId(traceId);
        walletWinDto.setTransactionId(traceId);
        walletWinDto.setUsername(gameSession.getAgentPlayerUsername());
        walletWinDto.setCurrency(gameSession.getCurrencyCode());
        walletWinDto.setToken(gameSession.getToken());
        walletWinDto.setExternalTransactionId(unsettledResultSettledData.getExternalTransactionId());
        walletWinDto.setReferenceTransactionId(referenceTransactionId);
        walletWinDto.setAmount((transferAmount == null) ? unsettledResultSettledData.getWinAmount() : transferAmount);
        walletWinDto.setGameCode(gameSession.getGameCode());
        walletWinDto.setRoundId(unsettledResultSettledData.getRoundId());
        walletWinDto.setWinType((winAmount.compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE);
        walletWinDto.setTimestamp(timestamp);

        return walletWinDto;
    }

    private WalletWinDto newWalletWinDtoForFullBetDto(String traceId, GameSession gameSession, RawSettledBet rawSettledBet, BigDecimal transferAmount) {

        BigDecimal winAmount = (rawSettledBet.getWinAmount() == null) ? BigDecimal.valueOf(0) : rawSettledBet.getWinAmount();

        WalletWinDto walletWinDto = new WalletWinDto();
        walletWinDto.setTraceId(traceId);
        walletWinDto.setTransactionId(traceId);
        walletWinDto.setUsername(gameSession.getAgentPlayerUsername());
        walletWinDto.setCurrency(gameSession.getCurrencyCode());
        walletWinDto.setToken(gameSession.getToken());
        walletWinDto.setExternalTransactionId(rawSettledBet.getExternalTransactionId());
        walletWinDto.setReferenceTransactionId(rawSettledBet.getInternalTransactionId());
        walletWinDto.setAmount(transferAmount);
        walletWinDto.setGameCode(gameSession.getGameCode());
        walletWinDto.setRoundId(rawSettledBet.getRoundId());
        walletWinDto.setWinType((winAmount.compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE);
        walletWinDto.setTimestamp(rawSettledBet.getVendorBetTime());
        return walletWinDto;
    }

    private WalletBetResultDto newWalletBetResultDtoForFullBetDto(String traceId, GameSession gameSession, RawSettledBet rawSettledBet) {

        BigDecimal winAmount = (rawSettledBet.getWinAmount() == null) ? BigDecimal.valueOf(0) : rawSettledBet.getWinAmount();

        WalletBetResultDto walletBetResultDto = new WalletBetResultDto();
        walletBetResultDto.setTraceId(traceId);
        walletBetResultDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBetResultDto.setTransactionId(rawSettledBet.getInternalTransactionId());
        walletBetResultDto.setExternalTransactionId(rawSettledBet.getExternalTransactionId());
        walletBetResultDto.setExternalRoundId(rawSettledBet.getRoundId());
        walletBetResultDto.setExternalBetId(rawSettledBet.getVendorBetId());
        walletBetResultDto.setBetAmount(rawSettledBet.getBetAmount());
        walletBetResultDto.setWinAmount(rawSettledBet.getWinAmount());
        walletBetResultDto.setEffectiveTurnover(rawSettledBet.getEffectiveTurnover());
        walletBetResultDto.setJackpotAmount(rawSettledBet.getJackpotAmount());
        walletBetResultDto.setWinLoss(rawSettledBet.getWinLoss());
        walletBetResultDto.setWinType((winAmount.compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE);
        walletBetResultDto.setIsFreespin(rawSettledBet.getIsFreespin());
        //TODO TO BE DECIDE TO CHANGE WITH SAME NAME?
        walletBetResultDto.setIsEndRound(rawSettledBet.getStatus());
        walletBetResultDto.setIsCancelled(rawSettledBet.getIsCancelled());
        walletBetResultDto.setCurrency(gameSession.getCurrencyCode());
        walletBetResultDto.setToken(gameSession.getToken());
        walletBetResultDto.setGameCode(gameSession.getGameCode());
        walletBetResultDto.setBetTime(rawSettledBet.getVendorBetTime());
        walletBetResultDto.setSettledTime(rawSettledBet.getVendorSettleTime());

        return walletBetResultDto;
    }

    private WalletBetDto newWalletBetForFullBetDto(String traceId, GameSession gameSession, RawSettledBet rawSettledBet) {
        WalletBetDto walletBetDto = new WalletBetDto();
        walletBetDto.setTraceId(traceId);
        walletBetDto.setTransactionId(traceId);
        walletBetDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBetDto.setCurrency(gameSession.getCurrencyCode());
        walletBetDto.setToken(gameSession.getToken());
        walletBetDto.setExternalTransactionId(rawSettledBet.getExternalTransactionId());
        walletBetDto.setAmount(rawSettledBet.getWinLoss().abs());
        walletBetDto.setGameCode(gameSession.getGameCode());
        walletBetDto.setRoundId(rawSettledBet.getRoundId());
        walletBetDto.setTimestamp(rawSettledBet.getVendorBetTime());

        return walletBetDto;
    }

    private BetResultLog newBetResultLog(String traceId, GameSession gameSession, WinData winData, BetHistory betHistory, WalletWinDto walletWinDto, String rawData) {
        BetResultLog betResultLog = new BetResultLog();

        betResultLog.setId(traceId);
        betResultLog.setBetHistoryId(walletWinDto.getReferenceTransactionId());
        betResultLog.setExternalTransactionId(walletWinDto.getExternalTransactionId());
        betResultLog.setRoundId(betHistory.getRoundId());
        betResultLog.setVendorGameId(gameSession.getVendorGameId());
        betResultLog.setVendorPlayerId(gameSession.getVendorPlayerId());
        betResultLog.setAgentPlayerId(gameSession.getAgentPlayerId());
        betResultLog.setAgentId(gameSession.getAgentId());
        betResultLog.setVendorLineId(gameSession.getVendorLineId());
        betResultLog.setCurrencyId(gameSession.getCurrencyId());
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

    private RawResultBet newResultBet(String traceId, GameSession gameSession, UnsettledResultSettledData unsettledResultSettledData,
                                      String rawData, RawUnsettledBet rawUnsettledBet) {

        RawResultBet rawResultBet = new RawResultBet();
        String md5RawData = DigestUtils.md5Hex(rawData);
        BigDecimal winLoss = unsettledResultSettledData.getWinAmount().subtract(rawUnsettledBet.getBetAmount());

        rawResultBet.setId(unsettledResultSettledData.getVendorBetId() + '_' + unsettledResultSettledData.getRoundId() + '_' + gameSession.getVendorLineId() + '_' + gameSession.getVendorPlayerId());
        rawResultBet.setInternalTransactionId(rawUnsettledBet.getInternalTransactionId());
        rawResultBet.setExternalTransactionId(unsettledResultSettledData.getExternalTransactionId());
        rawResultBet.setRoundId(unsettledResultSettledData.getRoundId());
        rawResultBet.setVendorGameId(gameSession.getVendorGameId());
        rawResultBet.setVendorPlayerId(gameSession.getVendorPlayerId());
        rawResultBet.setAgentPlayerId(gameSession.getAgentPlayerId());
        rawResultBet.setAgentId(gameSession.getAgentId());
        rawResultBet.setVendorLineId(gameSession.getVendorLineId());
        rawResultBet.setCurrencyId(gameSession.getCurrencyId());
        rawResultBet.setOperatorStatus(1);
        rawResultBet.setWinAmount(unsettledResultSettledData.getWinAmount());
        rawResultBet.setEffectiveTurnover(rawUnsettledBet.getBetAmount());
        rawResultBet.setWinLoss(winLoss);
        rawResultBet.setVendorWinLoss(winLoss);
        //if WinAmount >= 0 then resultType will be win
        rawResultBet.setResultType((rawResultBet.getWinAmount().compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN.code : WinType.LOSE.code);
        rawResultBet.setMd5RawSettledResult(md5RawData);
        rawResultBet.setResultTime(unsettledResultSettledData.getResultTime());
        rawResultBet.setVendorSettleTime(unsettledResultSettledData.getVendorSettleTime());
        rawResultBet.setRefundAmount(unsettledResultSettledData.getRefundAmount());
        rawResultBet.setBetAmount(rawUnsettledBet.getBetAmount());
        rawResultBet.setJackpotAmount(unsettledResultSettledData.getJackpotAmount());
        rawResultBet.setVendorBetId(unsettledResultSettledData.getVendorBetId());
        rawResultBet.setIsCancelled(unsettledResultSettledData.getIsCancelled());
        rawResultBet.setIsFreespin(unsettledResultSettledData.getIsFreespin());

        return rawResultBet;
    }

    private RawSettledBet newSettledBet(String traceId, GameSession gameSession, UnsettledResultSettledData unsettledResultSettledData)
            throws MergedBetDataIntegrityException {

        try {
            RawSettledBet rawSettledBet = new RawSettledBet();
            BeanUtils.copyProperties(rawSettledBet, unsettledResultSettledData);

            rawSettledBet.setId(unsettledResultSettledData.getVendorBetId() + '_' + unsettledResultSettledData.getRoundId() + '_' + gameSession.getVendorLineId() + '_' + gameSession.getVendorPlayerId());
            //will get from unsettled bet or result bet for the internalTransactionId
            //rawSettledBet.setInternalTransactionId(traceId);
            rawSettledBet.setVendorGameId(gameSession.getVendorGameId());
            rawSettledBet.setVendorPlayerId(gameSession.getVendorPlayerId());
            rawSettledBet.setAgentPlayerId(gameSession.getAgentPlayerId());
            rawSettledBet.setAgentId(gameSession.getAgentId());
            rawSettledBet.setVendorLineId(gameSession.getVendorLineId());
            rawSettledBet.setCurrencyId(gameSession.getCurrencyId());

            return rawSettledBet;

        } catch (IllegalAccessException illegalAccessException) {
            throw new MergedBetDataIntegrityException("getValueFromObject invalid : " + illegalAccessException.getMessage());

        } catch (InvocationTargetException invocationTargetException) {
            throw new MergedBetDataIntegrityException("copyProperties invalid : " + invocationTargetException.getMessage());
        }

    }

    private RawSettledBet newUnsettleResultSettledBet(String traceId, GameSession gameSession,
                                                      UnsettledResultSettledData unsettledResultSettledData, String rawData) {

        RawSettledBet rawSettledBet = new RawSettledBet();
        String md5RawData = DigestUtils.md5Hex(rawData);

        rawSettledBet.setId(unsettledResultSettledData.getVendorBetId() + '_' + unsettledResultSettledData.getRoundId() + '_' + gameSession.getVendorLineId() + '_' + gameSession.getVendorPlayerId());
        rawSettledBet.setInternalTransactionId(traceId);
        rawSettledBet.setExternalTransactionId(unsettledResultSettledData.getExternalTransactionId());
        rawSettledBet.setRoundId(unsettledResultSettledData.getRoundId());
        rawSettledBet.setVendorGameId(gameSession.getVendorGameId());
        rawSettledBet.setVendorPlayerId(gameSession.getVendorPlayerId());
        rawSettledBet.setVendorId(gameSession.getVendorId());
        rawSettledBet.setVendorLineId(gameSession.getVendorLineId());
        rawSettledBet.setAgentPlayerId(gameSession.getAgentPlayerId());
        rawSettledBet.setAgentId(gameSession.getAgentId());
        rawSettledBet.setGameCategoryId(gameSession.getGameCategoryId());
        rawSettledBet.setCurrencyId(gameSession.getCurrencyId());
        rawSettledBet.setBetAmount(unsettledResultSettledData.getBetAmount());
        rawSettledBet.setWinAmount(unsettledResultSettledData.getWinAmount());
        rawSettledBet.setWinLoss(unsettledResultSettledData.getWinLoss());
        rawSettledBet.setVendorWinLoss(unsettledResultSettledData.getVendorWinLoss());
        rawSettledBet.setEffectiveTurnover(unsettledResultSettledData.getEffectiveTurnover());
        rawSettledBet.setRefundAmount(unsettledResultSettledData.getRefundAmount());
        rawSettledBet.setResultType(unsettledResultSettledData.getResultType().code);
        rawSettledBet.setMd5RawSettledResult(md5RawData);
        rawSettledBet.setResettleNum(0);
        rawSettledBet.setGameSessionToken(gameSession.getToken());
        rawSettledBet.setVendorBetTime(unsettledResultSettledData.getVendorBetTime());
        rawSettledBet.setVendorSettleTime(unsettledResultSettledData.getVendorSettleTime());
        rawSettledBet.setResultTime(unsettledResultSettledData.getResultTime());
        rawSettledBet.setVendorBetId(unsettledResultSettledData.getVendorBetId());
        rawSettledBet.setIsCancelled(unsettledResultSettledData.getIsCancelled());
        rawSettledBet.setIsFreespin(unsettledResultSettledData.getIsFreespin());
        rawSettledBet.setJackpotAmount(unsettledResultSettledData.getJackpotAmount());

        return rawSettledBet;
    }

    private WalletRefundDto newWalletRefundDto(String traceId, GameSession gameSession, Long currentTimestamp, BetHistory betHistory) {
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

    private WalletBalanceDto newWalletBalanceDto(String traceId, GameSession gameSession) {
        WalletBalanceDto walletBalanceDto = new WalletBalanceDto();
        walletBalanceDto.setTraceId(traceId);
        walletBalanceDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBalanceDto.setCurrency(gameSession.getCurrencyCode());
        walletBalanceDto.setToken(gameSession.getToken());

        return walletBalanceDto;
    }

    private WalletBalanceVo sendSettledWalletTransaction(Integer agentId, String traceId, GameSession gameSession, RawSettledBet rawSettledBet, Boolean isFullBet)
            throws InvalidAgentApiCredentialException, InvalidOperatorResponseException, InsufficientBalanceException {

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String callbackUrl = agentApiCredential.getCallbackUrl();
        WalletBalanceVo balanceVo;

        if (isFullBet == true) {
            //if isFullBet = true, then we will compare with using winLoss to decide send as wallet/win or lose
            if ((rawSettledBet.getWinLoss().compareTo(BigDecimal.ZERO) >= 0)) {
                //if WinLoss >= 0 then we will send as win
                WalletWinDto walletWinDto = this.newWalletWinDtoForFullBetDto(traceId, gameSession, rawSettledBet, rawSettledBet.getWinLoss());
                String signature = authenticationService.generateSignature(walletWinDto, agentApiCredential.getApiSecret());
                balanceVo = walletWinAction.call(callbackUrl, signature, walletWinDto);

            } else {
                //else send as lose
                WalletBetDto walletBetDto = this.newWalletBetForFullBetDto(traceId, gameSession, rawSettledBet);
                String signature = authenticationService.generateSignature(walletBetDto, agentApiCredential.getApiSecret());
                balanceVo = walletBetAction.call(callbackUrl, signature, walletBetDto);
            }
        } else {
            //else isFullBet = false, then we will send as wallet/win with winAmount (because bet already deducted)
            WalletWinDto walletWinDto = this.newWalletWinDtoForFullBetDto(traceId, gameSession, rawSettledBet, rawSettledBet.getWinAmount());
            String signature = authenticationService.generateSignature(walletWinDto, agentApiCredential.getApiSecret());
            balanceVo = walletWinAction.call(callbackUrl, signature, walletWinDto);
        }

        return balanceVo;
    }

    //temporary updated version for sendSettledWalletTransaction function, avoid changes update to stg environment
    private WalletBalanceVo sendSettledWalletTransactionPlus(Integer agentId, String traceId, GameSession gameSession, RawSettledBet rawSettledBet, Boolean isFullBet)
            throws InvalidAgentApiCredentialException, InvalidOperatorResponseException, InsufficientBalanceException {

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String callbackUrl = agentApiCredential.getCallbackUrl();
        WalletBalanceVo balanceVo;

        if (isFullBet == true) {
            //if isFullBet = true, then we will compare with using winLoss to decide send as wallet/win or lose
            WalletBetResultDto walletBetResultDto = this.newWalletBetResultDtoForFullBetDto(traceId, gameSession, rawSettledBet);
            String signature = authenticationService.generateSignature(walletBetResultDto, agentApiCredential.getApiSecret());
            balanceVo = walletBetResultAction.call(callbackUrl, signature, walletBetResultDto);

        } else {
            //else isFullBet = false, then we will send as wallet/win with winAmount (because bet already deducted)
            WalletWinDto walletWinDto = this.newWalletWinDtoForFullBetDto(traceId, gameSession, rawSettledBet, rawSettledBet.getWinAmount());
            String signature = authenticationService.generateSignature(walletWinDto, agentApiCredential.getApiSecret());
            balanceVo = walletWinAction.call(callbackUrl, signature, walletWinDto);

        }

        return balanceVo;
    }
}
