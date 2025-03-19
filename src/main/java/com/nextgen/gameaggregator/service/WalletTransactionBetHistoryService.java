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

    public void update(WalletRequest walletRequest, GameSession gameSession) {

        String id = walletRequest.getRoundId();
        rawWalletTransactionBetHistoryRepository.findById(id).ifPresent(existingRecord -> {
            BigDecimal winLoss = walletRequest.getWinLoss();

            if (winLoss == null && walletRequest.getBetAmount() != null && walletRequest.getWinAmount() != null) {
                winLoss = walletRequest.getWinAmount().subtract(walletRequest.getBetAmount());
            }
            existingRecord.setWinLoss(winLoss);
            existingRecord.setWinAmount(walletRequest.getTransferAmount());

            assert winLoss != null;
            if (winLoss.compareTo(BigDecimal.ZERO) < 0) {
                existingRecord.setStatus(4);
            } else if (winLoss.compareTo(BigDecimal.ZERO) > 0) {
                existingRecord.setStatus(2);
            } else {
                existingRecord.setStatus(3);
            }
            rawWalletTransactionBetHistoryRepository.save(existingRecord);
        });
    }

    public Integer findById(String roundId) {
        return rawWalletTransactionBetHistoryRepository.findById(roundId)
                .map(RawWalletTransactionBetHistory::getStatus)
                .orElse(null);
    }
}


