package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.RawWalletTransactionBetHistory;
import com.nextgen.gameaggregator.repository.ga.writer.RawWalletTransactionBetHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class WalletTransactionBetHistoryService {

    private final RawWalletTransactionBetHistoryRepository rawWalletTransactionBetHistoryRepository;

    public WalletTransactionBetHistoryService(RawWalletTransactionBetHistoryRepository rawWalletTransactionBetHistoryRepository) {
        this.rawWalletTransactionBetHistoryRepository = rawWalletTransactionBetHistoryRepository;
    }

    public void create(WalletRequest walletRequest, GameSession gameSession) {

        RawWalletTransactionBetHistory rawWalletTransactionBetHistory = new RawWalletTransactionBetHistory();

        rawWalletTransactionBetHistory.setId(walletRequest.getRoundId());
        rawWalletTransactionBetHistory.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        rawWalletTransactionBetHistory.setVendorBetId(walletRequest.getBetId());
        rawWalletTransactionBetHistory.setRoundId(walletRequest.getRoundId());
        rawWalletTransactionBetHistory.setExternalTransactionId(walletRequest.getExternalTransactionId());
        rawWalletTransactionBetHistory.setToken(gameSession.getToken());
        rawWalletTransactionBetHistory.setBetAmount(walletRequest.getTransferAmount());
        rawWalletTransactionBetHistory.setWinAmount(BigDecimal.ZERO);
        rawWalletTransactionBetHistory.setStatus(1);
        rawWalletTransactionBetHistory.setCreateDate(System.currentTimeMillis());
        rawWalletTransactionBetHistoryRepository.save(rawWalletTransactionBetHistory);

    }

    //status 0 = bet fail ,1= bet sucess ,2= credit _success ,3= rollback_sucess
    public void update(WalletRequest walletRequest) {

        String id = walletRequest.getRoundId();
        rawWalletTransactionBetHistoryRepository.findById(id).ifPresent(existingRecord -> {
            BigDecimal winLoss = walletRequest.getWinLoss();

            if (winLoss == null && walletRequest.getBetAmount() != null && walletRequest.getWinAmount() != null) {
                winLoss = walletRequest.getWinAmount().subtract(walletRequest.getBetAmount());
            }
            if (walletRequest.getIsRefund().equals(1)) {
                existingRecord.setStatus(3);
            } else {
                existingRecord.setStatus(2);
            }
            existingRecord.setWinLoss(winLoss);
            existingRecord.setWinAmount(walletRequest.getTransferAmount());

            rawWalletTransactionBetHistoryRepository.save(existingRecord);
        });
    }

    public RawWalletTransactionBetHistory findWalletTransactionBetHistory(String orderId) {
        return rawWalletTransactionBetHistoryRepository.findById(orderId).orElse(null);
    }
}


