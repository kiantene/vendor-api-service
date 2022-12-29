package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceDto;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.bet.*;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinAction;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinDto;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinVo;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
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
    private WalletBalanceAction walletBalanceAction;
    @Autowired
    private WalletBetAction walletBetAction;
    @Autowired
    private WalletWinAction walletWinAction;

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

        // TODO: implement error handling
        return responseVo.getData().getBalance();
    }

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

        if (responseVo.getStatus().equals(ResponseCodes.SUCCESS)) {
            BetHistory betHistory = new BetHistory();
            betHistory.setId(traceId);
            betHistory.setExternalTransactionId(walletBetDto.getExternalTransactionId());
            betHistory.setRoundId(walletBetDto.getRoundId());
            betHistory.setVendorGameId(gameSession.getVendorGameId());
            betHistory.setVendorPlayerId(gameSession.getVendorPlayerId());
            betHistory.setVendorId(0);
            betHistory.setAgentPlayerId(gameSession.getAgentPlayerId());
            betHistory.setAgentId(gameSession.getAgentId());
            betHistory.setMasterAgentId(0);
            betHistory.setHouseId(0);
            betHistory.setGameCategoryId(0);
            betHistory.setCurrencyId(0);
            betHistory.setBetAmount(walletBetDto.getAmount());
            betHistory.setWinAmount(BigDecimal.ZERO);
            betHistory.setWinLoss(BigDecimal.ZERO);
            betHistory.setVendorWinLoss(BigDecimal.ZERO);
            betHistory.setEffectiveTurnover(BigDecimal.ZERO);
            betHistory.setResultType(0);
            betHistory.setStatus(1);
            betHistory.setVendorBetTime(walletBetDto.getTimestamp());
            betHistory.setCreateDate(System.currentTimeMillis());

            betHistoryRepository.save(betHistory);
        }

        return responseVo.getData().getBalance();
    }

    public BigDecimal processWin(String traceId, GameSession gameSession, BetData betData) {
        Integer agentId = gameSession.getAgentId();
        String callbackUrl = agentApiCredentialService.getCallbackUrl(agentId);
        String signature = ""; // TODO: implement signature generation

        WalletWinDto walletWinDto = new WalletWinDto();
        walletWinDto.setTraceId(traceId);
        walletWinDto.setTransactionId(traceId);
        walletWinDto.setUsername(gameSession.getAgentPlayerUsername());
        walletWinDto.setCurrency(gameSession.getCurrencyCode());
        walletWinDto.setToken(gameSession.getToken());
        walletWinDto.setExternalTransactionId(betData.getExternalTransactionId());
        walletWinDto.setReferenceTransactionId(null);
        walletWinDto.setAmount(betData.getAmount());
        walletWinDto.setGameId(betData.getGameId()); // TODO: game code mapping
        walletWinDto.setRoundId(betData.getRoundId());
        walletWinDto.setWinType(WalletWinAction.TYPE_WIN);
        walletWinDto.setTimestamp(betData.getTimestamp());

        WalletWinVo responseVo = walletWinAction.call(callbackUrl, signature, walletWinDto);

        return responseVo.getData().getBalance();
    }
}
