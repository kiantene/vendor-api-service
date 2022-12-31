package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.DuplicateExternalTransactionIdException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.*;
import com.nextgen.gameaggregator.operator.wallet.bet.*;
import com.nextgen.gameaggregator.operator.wallet.win.*;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.BetResultLogRepository;
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
    private BetHistoryRepository betHistoryRepository;
    @Autowired
    private BetResultLogRepository betResultLogRepository;
    @Autowired
    private WalletBalanceAction walletBalanceAction;
    @Autowired
    private WalletBetAction walletBetAction;
    @Autowired
    private WalletWinAction walletWinAction;
    @Autowired
    private BetHistoryService betHistoryService;

    public BigDecimal getBalance(String traceId, GameSession gameSession) throws InvalidOperatorResponseException {
        Integer agentId = gameSession.getAgentId();
        String callbackUrl = agentApiCredentialService.getCallbackUrl(agentId);
        String signature = ""; // TODO: implement signature generation

        WalletBalanceDto walletBalanceDto = new WalletBalanceDto();
        walletBalanceDto.setTraceId(traceId);
        walletBalanceDto.setUsername(gameSession.getAgentPlayerUsername());
        walletBalanceDto.setCurrency(gameSession.getCurrencyCode());
        walletBalanceDto.setToken(gameSession.getToken());

        WalletBalanceVo responseVo = walletBalanceAction.call(callbackUrl, signature, walletBalanceDto);

        // TODO: to handle balance returned with more than 4 decimals
        // TODO: implement error handling
        return responseVo.getData().getBalance();
    }

    /**
     * To process the bet by sending the bet data to Operator to validate the player has sufficient balance
     * to place the bet. When the Operator has responded with sufficient balance, we will save a record of the bet
     * as Unsettled.
     *
     * @param traceId A unique Id for this request
     * @param gameSession GameSession object containing information of the vendor, game, player
     * @param betData BetData object containing information of the bet such as betAmount, game, betTime
     * @return The player's current wallet balance after deducting the bet amount.
     */
    public BigDecimal processBet(String traceId, GameSession gameSession, BetData betData) {
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

        WalletBetVo responseVo = walletBetAction.call(callbackUrl, signature, walletBetDto);

        if (responseVo.getStatus() == ResponseCodes.Status.SC_OK) {
            BetHistory betHistory = new BetHistory();
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
            betHistory.setWinAmount(BigDecimal.ZERO);
            betHistory.setWinLoss(BigDecimal.ZERO);
            betHistory.setVendorWinLoss(BigDecimal.ZERO);
            betHistory.setEffectiveTurnover(BigDecimal.ZERO);
            betHistory.setResultType(WinType.LOSE.code);
            betHistory.setStatus(BetStatus.UNSETTLED.code);
            betHistory.setVendorBetTime(walletBetDto.getTimestamp());
            betHistory.setCreateDate(System.currentTimeMillis());

            betHistoryRepository.save(betHistory);
        }

        return responseVo.getData().getBalance();
    }

    public BetResultLog processWin(String traceId, GameSession gameSession, WinData winData, String rawData) throws BetNotFoundException, DuplicateExternalTransactionIdException {
        Integer agentId = gameSession.getAgentId();
        Integer vendorGameId = gameSession.getVendorGameId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String roundId = winData.getRoundId();

        // 1. Check for duplicate transaction Id
        betHistoryService.checkDuplicateExternalTransaction(winData.getExternalTransactionId(), vendorGameId, vendorPlayerId);

        // 2. Retrieve the bet transaction
        BetHistory betHistory = betHistoryService.getBetTransaction(roundId, vendorGameId, vendorPlayerId);

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

        WalletWinVo responseVo = walletWinAction.call(callbackUrl, signature, walletWinDto);

        BetResultLog betResultLog = new BetResultLog();
        if (responseVo.getStatus() == ResponseCodes.Status.SC_OK) {
            BigDecimal balance = responseVo.getData().getBalance(); // TODO: check for null

            betResultLog.setId(traceId);
            betResultLog.setReferenceTransactionId(walletWinDto.getReferenceTransactionId());
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
            betResultLog.setStatus(1); // TODO: refactor, map to constant/enum value
            betResultLog.setVendorTime(walletWinDto.getTimestamp());
            betResultLog.setCreateDate(System.currentTimeMillis());

            betResultLogRepository.save(betResultLog);
        }

        return betResultLog;
    }
}
