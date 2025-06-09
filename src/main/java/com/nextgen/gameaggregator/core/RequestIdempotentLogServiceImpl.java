package com.nextgen.gameaggregator.core;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.repository.ga.writer.RequestIdempotentLogRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class RequestIdempotentLogServiceImpl implements RequestIdempotentLogService {
    private final RequestIdempotentLogRepository requestIdempotentLogRepository;

    public RequestIdempotentLogServiceImpl(RequestIdempotentLogRepository requestIdempotentLogRepository) {
        this.requestIdempotentLogRepository = requestIdempotentLogRepository;
    }

    @Override
    public String generateRequestIdempotentLogId(BetResultData betResultData, String vendorPlayerUsername) {
        String externalTransactionId = (betResultData.getExternalTransactionId() == null) ? "" : betResultData.getExternalTransactionId();
        vendorPlayerUsername = (vendorPlayerUsername == null) ? "" : vendorPlayerUsername;

        return externalTransactionId + "_" + vendorPlayerUsername;
    }

    @Override
    public String generateRollbackRequestIdempotentLogId(RollbackData rollbackData, String vendorPlayerUsername) {
        String externalTransactionId = (rollbackData.getRollbackId() == null) ? "" : rollbackData.getRollbackId();
        vendorPlayerUsername = (vendorPlayerUsername == null) ? "" : vendorPlayerUsername;

        return "rollback_" + externalTransactionId + "_" + vendorPlayerUsername;
    }

    @Override
    @CacheEvict(value = "RequestIdempotentLog", key = "{#betResultData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public void delete(BetResultData betResultData, String vendorPlayerUsername) {
        try {
            String id = this.generateRequestIdempotentLogId(betResultData, vendorPlayerUsername);
            requestIdempotentLogRepository.deleteById(id);
        } catch (Exception e) {
            // Handle exception for document not found, but do nothing
        }
    }

    @Override
    @CacheEvict(value = "RollbackRequestIdempotentLog", key = "{#rollbackData.rollbackId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public void delete(RollbackData rollbackData, String vendorPlayerUsername) {
        try {
            String id = this.generateRollbackRequestIdempotentLogId(rollbackData, vendorPlayerUsername);
            requestIdempotentLogRepository.deleteById(id);
        } catch (Exception e) {
            // Handle exception for document not found, but do nothing
        }
    }

    @Override
    @Cacheable(value = "RequestIdempotentLog", key = "{#betResultData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager", unless = "#result == null")
    public RequestIdempotentLog checkExists(BetResultData betResultData, String vendorPlayerUsername) throws TransactionStillProcessingException {
        String id = this.generateRequestIdempotentLogId(betResultData, vendorPlayerUsername);
        return requestIdempotentLogRepository.findById(id).orElse(null);
    }

    @Override
    @Cacheable(value = "RollbackRequestIdempotentLog", key = "{#rollbackData.rollbackId, #vendorPlayerUsername}", cacheManager = "cacheManager", unless = "#result == null")
    public RequestIdempotentLog checkExists(RollbackData rollbackData, String vendorPlayerUsername) throws TransactionStillProcessingException {
        String id = this.generateRollbackRequestIdempotentLogId(rollbackData, vendorPlayerUsername);
        return requestIdempotentLogRepository.findById(id).orElse(null);
    }

    @Override
    @CachePut(value = "RequestIdempotentLog", key = "{#betResultData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public RequestIdempotentLog create(BetResultData betResultData, String vendorPlayerUsername) {
        String id = this.generateRequestIdempotentLogId(betResultData, vendorPlayerUsername);
        RequestIdempotentLog createRequestIdempotentLog = new RequestIdempotentLog();
        createRequestIdempotentLog.setId(id);
        createRequestIdempotentLog.setCreateTime(System.currentTimeMillis());
        requestIdempotentLogRepository.save(createRequestIdempotentLog);
        return createRequestIdempotentLog;
    }

    @Override
    @CachePut(value = "RollbackRequestIdempotentLog", key = "{#rollbackData.rollbackId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public RequestIdempotentLog create(RollbackData rollbackData, String vendorPlayerUsername) {
        String id = this.generateRollbackRequestIdempotentLogId(rollbackData, vendorPlayerUsername);
        RequestIdempotentLog createRequestIdempotentLog = new RequestIdempotentLog();
        createRequestIdempotentLog.setId(id);
        createRequestIdempotentLog.setCreateTime(System.currentTimeMillis());
        requestIdempotentLogRepository.save(createRequestIdempotentLog);
        return createRequestIdempotentLog;
    }

    @Override
    @CachePut(value = "RequestIdempotentLog", key = "#requestIdempotentLog.id", cacheManager = "cacheManager")
    public RequestIdempotentLog save(RequestIdempotentLog requestIdempotentLog) {
        return requestIdempotentLogRepository.save(requestIdempotentLog);
    }

    @Override
    @Cacheable(value = "RequestIdempotentLog", key = "#id", cacheManager = "cacheManager")
    public RequestIdempotentLog get(String id) {
        return requestIdempotentLogRepository.findById(id).orElse(null);
    }

    // Sportsbook specific methods
    @Override
    public String generateBetResultRequestIdempotentLogId(String externalTransactionId, String vendorPlayerUsername, String prefix) {
        externalTransactionId = (externalTransactionId == null) ? "" : externalTransactionId;
        vendorPlayerUsername = (vendorPlayerUsername == null) ? "" : vendorPlayerUsername;

        if (prefix == null || prefix.isBlank()) {
            return externalTransactionId + "_" + vendorPlayerUsername;
        }
        return prefix + "_" + externalTransactionId + "_" + vendorPlayerUsername;
    }

    @Override
    @CacheEvict(value = "RequestIdempotentLog", key = "{#externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public void delete(String externalTransactionId, String vendorPlayerUsername, String prefix) {
        try {
            String id = this.generateBetResultRequestIdempotentLogId(externalTransactionId, vendorPlayerUsername, prefix);
            requestIdempotentLogRepository.deleteById(id);
        } catch (Exception e) {
            // Handle exception for document not found, but do nothing
        }
    }

    @Override
    @CachePut(value = "RequestIdempotentLog", key = "{#externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public RequestIdempotentLog create(String externalTransactionId, String vendorPlayerUsername, Integer walletRequestStatus, String prefix) {
        String id = this.generateBetResultRequestIdempotentLogId(externalTransactionId, vendorPlayerUsername, prefix);
        RequestIdempotentLog createRequestIdempotentLog = new RequestIdempotentLog();
        createRequestIdempotentLog.setId(id);
        createRequestIdempotentLog.setCreateTime(System.currentTimeMillis());
        createRequestIdempotentLog.setWalletRequestStatus(walletRequestStatus);
        requestIdempotentLogRepository.save(createRequestIdempotentLog);
        return createRequestIdempotentLog;
    }

    @Override
    @Cacheable(value = "RequestIdempotentLog", key = "{#externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager", unless = "#result == null")
    public RequestIdempotentLog getSportsRequestIdempotentLog(String externalTransactionId, String vendorPlayerUsername,  String prefix) {
        String id = this.generateBetResultRequestIdempotentLogId(externalTransactionId, vendorPlayerUsername, prefix);
        return requestIdempotentLogRepository.findById(id).orElse(null);
    }
}
