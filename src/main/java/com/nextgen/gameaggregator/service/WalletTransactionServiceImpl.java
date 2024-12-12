package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.exception.InternalServerException;
import com.nextgen.gameaggregator.repository.ga.writer.WalletTransactionRepository;
import com.nextgen.gameaggregator.util.ValidationUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class WalletTransactionServiceImpl implements WalletTransactionService {
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletTransactionServiceImpl(WalletTransactionRepository walletTransactionRepository) {
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Override
    public WalletTransaction prepareEntity(WalletRequest walletRequest, String action) {
        WalletTransaction walletTransaction = new WalletTransaction();

        walletTransaction.setVendorId(walletRequest.getVendorId());
        walletTransaction.setVendorPlayerUsername(walletRequest.getVendorPlayerUsername());
        walletTransaction.setToken(walletRequest.getToken());
        walletTransaction.setVendorGameCode(walletRequest.getVendorGameCode());
        walletTransaction.setCurrencyId(walletRequest.getCurrencyId());
        walletTransaction.setExternalTransactionId(walletRequest.getExternalTransactionId());
        walletTransaction.setVendorBetId(walletRequest.getVendorBetId());
        walletTransaction.setRoundId(walletRequest.getRoundId());
        walletTransaction.setAction(action);
        walletTransaction.setTakeAll(walletRequest.getTakeAll());
        walletTransaction.setTransferAmount(walletRequest.getTransferAmount());
        walletTransaction.setTransactionId(walletRequest.getTransactionId());
        walletTransaction.setBetId(walletRequest.getBetId());
        walletTransaction.setTimestamp(walletRequest.getTimestamp());
        walletTransaction.setCreatedDate(System.currentTimeMillis());
        walletTransaction.setId(walletRequest.getTraceId());

        return walletTransaction;
    }

    @Override
    @CachePut(value = "WalletTransaction", key = "{#walletTransaction.roundId, #walletTransaction.vendorPlayerUsername}", cacheManager = "cacheManager")
    public WalletTransaction save(WalletTransaction walletTransaction) throws InternalServerException {
        ValidationUtils.doValidation(walletTransaction, InternalServerException::new);
        walletTransactionRepository.save(walletTransaction);
        return walletTransaction;
    }

    @Override
    public WalletTransaction updateOperatorStatus(String id, WalletRequest walletRequest) {
        WalletTransaction walletTransaction = new WalletTransaction();
        walletTransaction.setId(id);
        walletTransaction.setOperatorStatus(walletRequest.getOperatorResponseStatus().code);
        walletTransaction.setBalance(walletRequest.getBalanceAfter());
        walletTransactionRepository.save(walletTransaction);
        return walletTransaction;
    }

    @Override
    @Cacheable(value = "WalletTransaction", key = "{#roundId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public WalletTransaction getByRoundIdAndVendorPlayerUsername(String roundId, String vendorPlayerUsername) {

        List<WalletTransaction> walletTransactionList = walletTransactionRepository.findByRoundIdAndVendorPlayerUsername(roundId, vendorPlayerUsername);

        if (walletTransactionList.isEmpty()) {
            return null;
        } else {
            return walletTransactionList.stream()
                    .max(Comparator.comparingLong(WalletTransaction::getCreatedDate))
                    .get();
        }
    }

    @Override
    @Cacheable(value = "WalletTransaction", key = "{#vendorId, #externalTransactionId}", cacheManager = "cacheManager")
    public WalletTransaction getByVendorIdAndExternalTransactionId(Integer vendorId, String externalTransactionId) {
        return walletTransactionRepository.findByVendorIdAndExternalTransactionId(vendorId, externalTransactionId);
    }
}
