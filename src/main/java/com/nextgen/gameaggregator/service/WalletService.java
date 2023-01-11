package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.BetOperatorFailEvent;
import com.nextgen.gameaggregator.eventing.events.BetRefundEvent;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
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

    public BigDecimal getBalance(String traceId, String username) throws InvalidPlayerException, InvalidAgentApiCredentialException {
        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(username);
        AgentPlayer agentPlayer;

        try {
            agentPlayer = agentPlayerService.get(vendorPlayer.getAgentPlayerId());
        } catch (RecordNotFoundException recordNotFoundException) {
            throw new InvalidPlayerException();
        }

        Integer agentId = agentPlayer.getAgentId();
        String callbackUrl = agentApiCredentialService.getAgentApiCredential(agentId).getCallbackUrl();
        String signature = ""; // TODO: implement signature generation

        WalletBalanceDto walletBalanceDto = new WalletBalanceDto();
        walletBalanceDto.setTraceId(traceId);
        walletBalanceDto.setUsername(agentPlayer.getUsername());
        walletBalanceDto.setCurrency("CNY"); // TODO: to get from game session
        walletBalanceDto.setToken(""); // TODO: to get from game session

        WalletBalanceVo balanceVo = walletBalanceAction.call(callbackUrl, signature, walletBalanceDto);


        // TODO: to handle balance returned with more than 4 decimals
        // TODO: implement error handling
        return balanceVo.getData().getBalance();
    }

    public BigDecimal getBalance(String traceId, GameSession gameSession) throws InvalidOperatorResponseException, InvalidAgentApiCredentialException {
        Integer agentId = gameSession.getAgentId();
        String callbackUrl = agentApiCredentialService.getAgentApiCredential(agentId).getCallbackUrl();
        String signature = ""; // TODO: implement signature generation

        WalletBalanceDto walletBalanceDto = new WalletBalanceDto();
        walletBalanceDto.setTraceId(traceId);
        walletBalanceDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBalanceDto.setCurrency(gameSession.getCurrencyCode());
        walletBalanceDto.setToken(gameSession.getToken());

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

        try {
            betHistoryService.create(betHistory);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            // 1. Check for duplicate transaction Id
            throw new DuplicateExternalTransactionIdException("Duplicate bet_history " +
                    ", external_transaction_id:" + betHistory.getExternalTransactionId() +
                    ", round_id:" + betHistory.getRoundId() +
                    ", vendor_line_id:" + betHistory.getVendorLineId());
        }

        try {
            WalletBalanceVo balanceVo = walletBetAction.stub();
//            WalletBalanceVo balanceVo = walletBetAction.call(callbackUrl, signature, walletBetDto);
            BetEvent betEvent = new BetEvent(betHistory, balanceVo.getData().getBalance());

            // TODO: check for null pointer
            // Emit event for additional asynchronous processing
            EventDispatcherSystem.emitAsync(betEvent);

            return betEvent;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            //Update bet_history operator status based on exception
            BetOperatorFailEvent betOperatorFailEvent =
                    new BetOperatorFailEvent(betHistory, ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            EventDispatcherSystem.emitAsync(betOperatorFailEvent);
            throw insufficientBalanceException;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //Update bet_history operator status based on exception
            BetOperatorFailEvent betOperatorFailEvent =
                    new BetOperatorFailEvent(betHistory, invalidOperatorResponseException.getOperatorStatus());
            EventDispatcherSystem.emitAsync(betOperatorFailEvent);
            throw invalidOperatorResponseException;
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
            BetNotFoundException, DuplicateExternalTransactionIdException, InvalidAgentApiCredentialException, InvalidOperatorResponseException {

        Integer agentId = gameSession.getAgentId();
        Integer vendorGameId = gameSession.getVendorGameId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String roundId = winData.getRoundId();

        // TODO: To discuss if Agent is disable, should system ignore callback and just insert to bet_result_log
        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String callbackUrl = agentApiCredential.getCallbackUrl();

        // 1. Retrieve the bet transaction
        BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(roundId, vendorGameId, vendorPlayerId);

        WalletWinDto walletWinDto = this.newWalletWinDto(traceId, gameSession, winData, betHistory);
        String signature = authenticationService.generateSignature(walletWinDto, agentApiCredential.getApiSecret());

        BetResultLog betResultLog = this.newBetResultLog(traceId, gameSession, winData, betHistory, walletWinDto, rawData);

        try {
            betResultLogService.create(betResultLog);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            // 2. Check for duplicate transaction Id
            throw new DuplicateExternalTransactionIdException("Duplicate bet_result_log " +
                    ", external_transaction_id:" + betResultLog.getExternalTransactionId() +
                    ", round_id:" + betResultLog.getRoundId() +
                    ", vendor_line_id:" + betResultLog.getVendorLineId());
        }

        try {
            WalletBalanceVo balanceVo = walletWinAction.call(callbackUrl, signature, walletWinDto);
            BetResultEvent betResultEvent = new BetResultEvent(betHistory, betResultLog, balanceVo.getData().getBalance());
            EventDispatcherSystem.emitAsync(betResultEvent);
            return betResultEvent;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //Update bet_history operator status based on exception
//            BetOperatorFailEvent betOperatorFailEvent =
//                    new BetOperatorFailEvent(betHistory, invalidOperatorResponseException.getOperatorStatus());
//            EventDispatcherSystem.emitAsync(betOperatorFailEvent);
            throw invalidOperatorResponseException;
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
            BetNotFoundException, RecordNotFoundException, InvalidAgentApiCredentialException {

        Integer vendorId = gameSession.getVendorId();
        Long currentTimestamp = System.currentTimeMillis();

        // 1. Retrieve the bet transaction
        BetHistory betHistory = betHistoryService.getBetTransactionByVendorTransactionId(externalTransactionId, vendorId);
        AgentPlayer agentPlayer = agentPlayerService.get(betHistory.getAgentPlayerId());

        // TODO: add caching for callback url
        String callbackUrl = agentApiCredentialService.getAgentApiCredential(betHistory.getAgentId()).getCallbackUrl();
        String signature = ""; // TODO: implement signature generation

        WalletRefundDto walletRefundDto = new WalletRefundDto();
        walletRefundDto.setTraceId(traceId);
        walletRefundDto.setTransactionId(traceId);
        walletRefundDto.setUsername(agentPlayer.getUsername());
        walletRefundDto.setExternalTransactionId(externalTransactionId);
        walletRefundDto.setReferenceTransactionId(betHistory.getId());
        walletRefundDto.setGameCode(betHistory.getVendorGameId().toString()); // TODO: update to correct game Id
        walletRefundDto.setRoundId(betHistory.getRoundId());
        walletRefundDto.setTimestamp(currentTimestamp);

        WalletBalanceVo balanceVo = walletRefundAction.call(callbackUrl, signature, walletRefundDto);

        BetRefundLog betRefundLog = new BetRefundLog();
        BigDecimal balance = null;
        if (balanceVo.getStatus() == ResponseCodes.Status.SC_OK) {
            balance = balanceVo.getData().getBalance(); // TODO: check for null

            betRefundLog.setBetHistoryId(betHistory.getId());
            betRefundLog.setExternalTransactionId(externalTransactionId);
            betRefundLog.setRoundId(betHistory.getRoundId());
            betRefundLog.setVendorGameId(betHistory.getVendorGameId());
            betRefundLog.setVendorPlayerId(betHistory.getVendorPlayerId());
            betRefundLog.setAgentPlayerId(betHistory.getAgentPlayerId());
            betRefundLog.setAgentId(betHistory.getAgentId());
            betRefundLog.setCurrencyId(betHistory.getCurrencyId());
            betRefundLog.setBalance(balance);
            betRefundLog.setRawData(rawData);
            betRefundLog.setStatus(1); // TODO: refactor, map to constant/enum value
            betRefundLog.setCreateTime(currentTimestamp);

            betRefundLogService.create(betRefundLog);
        } else {
            // TODO: throw exception
            log.error("ProcessRefund: " + balanceVo);
        }

        BetRefundEvent betRefundEvent = new BetRefundEvent(betHistory, betRefundLog, balance);
        // Emit event for additional asynchronous processing such as publishing data to a kafka topic
        EventDispatcherSystem.emitAsync(betRefundEvent);
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
        betResultLog.setWinAmount(walletWinDto.getAmount());
        betResultLog.setResultType(winData.getWinType().code);
        betResultLog.setBalance(new BigDecimal(0));
        betResultLog.setRawData(rawData);
        betResultLog.setVendorTime(walletWinDto.getTimestamp());

        return betResultLog;
    }
}
