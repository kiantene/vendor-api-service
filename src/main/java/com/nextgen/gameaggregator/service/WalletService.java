package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.BetRefundEvent;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.*;
import com.nextgen.gameaggregator.operator.wallet.bet.*;
import com.nextgen.gameaggregator.operator.wallet.refund.*;
import com.nextgen.gameaggregator.operator.wallet.win.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class WalletService {
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AgentPlayerService agentPlayerService;
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


    public BigDecimal getBalance(String traceId, GameSession gameSession) throws InvalidOperatorResponseException {
        Integer agentId = gameSession.getAgentId();
        String callbackUrl = agentApiCredentialService.getCallbackUrl(agentId);
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
     *
     * When the Operator has responded with sufficient balance, we will save a record of the bet
     * as Unsettled.
     *
     * @param traceId A unique Id for this request
     * @param gameSession GameSession object containing information of the vendor, game, player
     * @param betData BetData object containing information of the bet such as betAmount, game, betTime
     * @param rawData Raw data sent by vendor containing information of the bet
     * @return The player's current wallet balance after deducting the bet amount
     */
    public BetEvent processBet(String traceId, GameSession gameSession, BetData betData, String rawData) throws InsufficientBalanceException {
        Integer agentId = gameSession.getAgentId();
        String callbackUrl = agentApiCredentialService.getCallbackUrl(agentId);
        String signature = ""; // TODO: implement signature generation

        WalletBetDto walletBetDto = new WalletBetDto();
        walletBetDto.setTraceId(traceId);
        walletBetDto.setTransactionId(traceId);
        walletBetDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBetDto.setCurrency(gameSession.getCurrencyCode());
        walletBetDto.setToken(gameSession.getToken());
        walletBetDto.setExternalTransactionId(betData.getExternalTransactionId());
        walletBetDto.setAmount(betData.getAmount());
        walletBetDto.setGameId(betData.getGameId()); // TODO: game code mapping
        walletBetDto.setRoundId(betData.getRoundId());
        walletBetDto.setTimestamp(betData.getTimestamp());

        WalletBalanceVo balanceVo = walletBetAction.call(callbackUrl, signature, walletBetDto);

        BetHistory betHistory = new BetHistory();
        BigDecimal balance = BigDecimal.ZERO;
        if (balanceVo.getStatus() == ResponseCodes.Status.SC_OK) {
            balance = balanceVo.getData().getBalance();
            boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
            if (isNegativeBalance) throw new InsufficientBalanceException();

            betHistory.setId(traceId);
            betHistory.setExternalTransactionId(walletBetDto.getExternalTransactionId());
            betHistory.setRoundId(walletBetDto.getRoundId());
            betHistory.setVendorGameId(gameSession.getVendorGameId());
            betHistory.setVendorPlayerId(gameSession.getVendorPlayerId());
            betHistory.setVendorId(gameSession.getVendorId());
            betHistory.setAgentPlayerId(gameSession.getAgentPlayerId());
            betHistory.setAgentId(gameSession.getAgentId());
            betHistory.setMasterAgentId(0);
            betHistory.setHouseId(0);
            betHistory.setGameCategoryId(gameSession.getGameCategoryId());
            betHistory.setCurrencyId(gameSession.getCurrencyId());
            betHistory.setBetAmount(walletBetDto.getAmount());
            betHistory.setRawData(rawData);
            betHistory.setVendorBetTime(walletBetDto.getTimestamp());

            betHistoryService.create(betHistory);
        } else {
            // TODO: throw exception
            log.error("ProcessBet: " + balanceVo);
            // TODO: to decide whether to save bet record for insufficient balance
        }

        // TODO: check for null pointer
        return new BetEvent(betHistory, balance);
    }

    /**
     * To process the result of a bet by sending the bet result data to Operator so that the Operator can update
     * the player's balance.
     *
     * @param traceId A unique Id for this request
     * @param gameSession GameSession object containing information of the vendor, game, player
     * @param winData WinData object containing information of the bet result
     * @param rawData Raw data sent by vendor containing information of the bet result
     * @return BetResultEvent An event object containing Bet and Bet Result information as well as the last balance
     *                        that can be used for further processing, if required
     * @throws BetNotFoundException If no bet record is found
     * @throws DuplicateExternalTransactionIdException If vendor's transaction Id is found
     */
    public BetResultEvent processWin(String traceId, GameSession gameSession, WinData winData, String rawData) throws BetNotFoundException, DuplicateExternalTransactionIdException {
        Integer agentId = gameSession.getAgentId();
        Integer vendorGameId = gameSession.getVendorGameId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String roundId = winData.getRoundId();

        // 1. Check for duplicate transaction Id
        betHistoryService.checkDuplicateExternalTransaction(winData.getExternalTransactionId(), vendorGameId, vendorPlayerId);

        // 2. Retrieve the bet transaction
        BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(roundId, vendorGameId, vendorPlayerId);

        // TODO: add caching for callback url
        String callbackUrl = agentApiCredentialService.getCallbackUrl(agentId);
        String signature = ""; // TODO: implement signature generation

        WalletWinDto walletWinDto = new WalletWinDto();
        walletWinDto.setTraceId(traceId);
        walletWinDto.setTransactionId(traceId);
        walletWinDto.setUsername(gameSession.getAgentPlayerUsername());
        walletWinDto.setCurrency(gameSession.getCurrencyCode());
        walletWinDto.setToken(gameSession.getToken());
        walletWinDto.setExternalTransactionId(winData.getExternalTransactionId());
        walletWinDto.setReferenceTransactionId(betHistory.getId());
        walletWinDto.setAmount(winData.getAmount());
        walletWinDto.setGameId(winData.getGameId()); // TODO: game code mapping
        walletWinDto.setRoundId(roundId);
        walletWinDto.setWinType(winData.getWinType());
        walletWinDto.setTimestamp(winData.getTimestamp());

        WalletBalanceVo balanceVo = walletWinAction.call(callbackUrl, signature, walletWinDto);

        BetResultLog betResultLog = new BetResultLog();
        BigDecimal balance = null;
        if (balanceVo.getStatus() == ResponseCodes.Status.SC_OK) {
            balance = balanceVo.getData().getBalance(); // TODO: check for null

            betResultLog.setId(traceId);
            betResultLog.setBetHistoryId(walletWinDto.getReferenceTransactionId());
            betResultLog.setExternalTransactionId(walletWinDto.getExternalTransactionId());
            betResultLog.setRoundId(roundId);
            betResultLog.setVendorGameId(vendorGameId);
            betResultLog.setVendorPlayerId(vendorPlayerId);
            betResultLog.setAgentPlayerId(gameSession.getAgentPlayerId());
            betResultLog.setAgentId(gameSession.getAgentId());
            betResultLog.setCurrencyId(gameSession.getCurrencyId());
            betResultLog.setWinAmount(walletWinDto.getAmount());
            betResultLog.setResultType(winData.getWinType().code);
            betResultLog.setBalance(balance);
            betResultLog.setRawData(rawData);
            betResultLog.setVendorTime(walletWinDto.getTimestamp());

            betResultLogService.create(betResultLog);
        } else {
            // TODO: throw exception
            log.error("ProcessWin: " + balanceVo);
        }

        return new BetResultEvent(betHistory, betResultLog, balance);
    }

    /**
     * To process the reversal of a bet by sending the refund instruction to Operator so that the Operator can perform
     * a reversal and return the updated balance of the player.
     *
     * @param traceId A unique Id for this request
     * @param externalTransactionId Vendor's bet transaction Id of a previous bet record
     * @param vendorPlayer A VendorPlayer entity object containing information about the player
     * @param rawData Raw data sent by vendor containing information of the Refund
     * @return BetRefundEvent An event object containing Bet and Refund information to be used for further processing, if required
     * @throws BetNotFoundException If no bet record is found
     * @throws RecordNotFoundException Generic exception for orphan records
     */
    public BetRefundEvent processRefund(String traceId, String externalTransactionId, VendorPlayer vendorPlayer, String rawData) throws BetNotFoundException, RecordNotFoundException {
        Integer vendorId = vendorPlayer.getVendorId();
        Long currentTimestamp = System.currentTimeMillis();

        // 1. Retrieve the bet transaction
        BetHistory betHistory = betHistoryService.getBetTransactionByVendorTransactionId(externalTransactionId, vendorId);
        AgentPlayer agentPlayer = agentPlayerService.get(betHistory.getAgentPlayerId());

        // TODO: add caching for callback url
        String callbackUrl = agentApiCredentialService.getCallbackUrl(betHistory.getAgentId());
        String signature = ""; // TODO: implement signature generation

        WalletRefundDto walletRefundDto = new WalletRefundDto();
        walletRefundDto.setTraceId(traceId);
        walletRefundDto.setTransactionId(traceId);
        walletRefundDto.setUsername(agentPlayer.getUsername());
        walletRefundDto.setExternalTransactionId(externalTransactionId);
        walletRefundDto.setReferenceTransactionId(betHistory.getId());
        walletRefundDto.setGameId(betHistory.getVendorGameId().toString()); // TODO: update to correct game Id
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

        // TODO: to refactor currency
        return new BetRefundEvent(betHistory, betRefundLog, balance);
    }
}
