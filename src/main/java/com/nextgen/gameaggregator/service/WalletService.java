package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
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
import com.nextgen.gameaggregator.operator.wallet.refund.WalletRefundAction;
import com.nextgen.gameaggregator.operator.wallet.refund.WalletRefundDto;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinAction;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinDto;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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

    public BigDecimal getBalance(String traceId, GameSession gameSession) throws InvalidOperatorResponseException, InvalidAgentApiCredentialException {
        Integer agentId = gameSession.getAgentId();
        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String callbackUrl = agentApiCredential.getCallbackUrl();

        WalletBalanceDto walletBalanceDto = this.newWalletBalanceDto(traceId, gameSession);
        String signature = authenticationService.generateSignature(walletBalanceDto, agentApiCredential.getApiSecret());

        try {
            WalletBalanceVo balanceVo = walletBalanceAction.call(callbackUrl, signature, walletBalanceDto);
            // TODO: to handle balance returned with more than 4 decimals
            // TODO: implement error handling
            return balanceVo.getData().getBalance();
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            throw invalidOperatorResponseException;
        }
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
     * To process the result of a bet by sending the bet result data to Operator so that the Operator can update
     * the player's balance.
     *
     * @param traceId     A unique Id for this request
     * @param gameSession GameSession object containing information of the vendor, game, player
     * @param winData     WinData object containing information of the bet result
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

        WalletWinDto walletWinDto = this.newWalletWinDto(traceId, gameSession, winData, betHistory);

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

    private WalletWinDto newWalletWinDto(String traceId, GameSession gameSession, WinData winData, BetHistory betHistory) {
        WalletWinDto walletWinDto = new WalletWinDto();
        walletWinDto.setTraceId(traceId);
        walletWinDto.setTransactionId(traceId);
        walletWinDto.setUsername(gameSession.getAgentPlayerUsername());
        walletWinDto.setCurrency(gameSession.getCurrencyCode());
        walletWinDto.setToken(gameSession.getToken());
        walletWinDto.setExternalTransactionId(winData.getExternalTransactionId());
        walletWinDto.setReferenceTransactionId(betHistory.getId());
        walletWinDto.setAmount(winData.getAmount());
        walletWinDto.setGameCode(gameSession.getGameCode());
        walletWinDto.setRoundId(winData.getRoundId());
        walletWinDto.setWinType(winData.getWinType());
        walletWinDto.setTimestamp(winData.getTimestamp());
        return walletWinDto;
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
}
