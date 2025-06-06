package com.nextgen.gameaggregator.core;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentData;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleData;
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
    public String generateBetResultRequestIdempotentLogId(SportBetResultData sportBetResultData,
            String vendorPlayerUsername) {
        String externalTransactionId = (sportBetResultData.getExternalTransactionId() == null) ? "" : sportBetResultData.getExternalTransactionId();
        vendorPlayerUsername = (vendorPlayerUsername == null) ? "" : vendorPlayerUsername;

        return externalTransactionId + "_" + vendorPlayerUsername;
    }
    
    @Override
    public String generateRefundRequestIdempotentLogId(SportRefundData sportRefundData, String vendorPlayerUsername) {
        String externalTransactionId = (sportRefundData.getExternalTransactionId() == null) ? "" : sportRefundData.getExternalTransactionId();
        vendorPlayerUsername = (vendorPlayerUsername == null) ? "" : vendorPlayerUsername;

        return "rollback_" + externalTransactionId + "_" + vendorPlayerUsername;
    }

    @Override
    public String generateUnsettleRequestIdempotentLogId(SportUnsettleData sportUnsettleData, String vendorPlayerUsername) {
        String externalTransactionId = (sportUnsettleData.getExternalTransactionId() == null) ? "" : sportUnsettleData.getExternalTransactionId();
        vendorPlayerUsername = (vendorPlayerUsername == null) ? "" : vendorPlayerUsername;

        return "unsettle_" + externalTransactionId + "_" + vendorPlayerUsername;
    }

    @Override
    public String generateAdjustmentRequestIdempotentLogId(SportAdjustmentData sportAdjustmentData, String vendorPlayerUsername) {
        String externalTransactionId = (sportAdjustmentData.getExternalTransactionId() == null) ? "" : sportAdjustmentData.getExternalTransactionId();
        vendorPlayerUsername = (vendorPlayerUsername == null) ? "" : vendorPlayerUsername;

        return "adjustment_" + externalTransactionId + "_" + vendorPlayerUsername;
    }

    @Override
    @CacheEvict(value = "RequestIdempotentLog", key = "{#sportBetResultData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public void delete(SportBetResultData sportBetResultData, String vendorPlayerUsername) {
        try {
            String id = this.generateBetResultRequestIdempotentLogId(sportBetResultData, vendorPlayerUsername);
            requestIdempotentLogRepository.deleteById(id);
        } catch (Exception e) {
            // Handle exception for document not found, but do nothing
        }
    }

    @Override
    @CacheEvict(value = "RequestIdempotentLog", key = "{#sportRefundData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public void delete(SportRefundData sportRefundData, String vendorPlayerUsername) {
        try {
            String id = this.generateRefundRequestIdempotentLogId(sportRefundData, vendorPlayerUsername);
            requestIdempotentLogRepository.deleteById(id);
        } catch (Exception e) {
            // Handle exception for document not found, but do nothing
        }
    }

    @Override
    @CacheEvict(value = "RequestIdempotentLog", key = "{#sportUnsettleData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public void delete(SportUnsettleData sportUnsettleData, String vendorPlayerUsername) {
        try {
            String id = this.generateUnsettleRequestIdempotentLogId(sportUnsettleData, vendorPlayerUsername);
            requestIdempotentLogRepository.deleteById(id);
        } catch (Exception e) {
            // Handle exception for document not found, but do nothing
        }
    }

    @Override
    @CacheEvict(value = "RequestIdempotentLog", key = "{#sportAdjustmentData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public void delete(SportAdjustmentData sportAdjustmentData, String vendorPlayerUsername) {
        try {
            String id = this.generateAdjustmentRequestIdempotentLogId(sportAdjustmentData, vendorPlayerUsername);
            requestIdempotentLogRepository.deleteById(id);
        } catch (Exception e) {
            // Handle exception for document not found, but do nothing
        }
    }

    @Override
    @CachePut(value = "RequestIdempotentLog", key = "{#sportBetResultData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public RequestIdempotentLog create(SportBetResultData sportBetResultData, String vendorPlayerUsername) {
        String id = this.generateBetResultRequestIdempotentLogId(sportBetResultData, vendorPlayerUsername);
        RequestIdempotentLog createRequestIdempotentLog = new RequestIdempotentLog();
        createRequestIdempotentLog.setId(id);
        createRequestIdempotentLog.setCreateTime(System.currentTimeMillis());
        requestIdempotentLogRepository.save(createRequestIdempotentLog);
        return createRequestIdempotentLog;
    }

    @Override
    @CachePut(value = "RequestIdempotentLog", key = "{#sportRefundData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public RequestIdempotentLog create(SportRefundData sportRefundData, String vendorPlayerUsername) {
        String id = this.generateRefundRequestIdempotentLogId(sportRefundData, vendorPlayerUsername);
        RequestIdempotentLog createRequestIdempotentLog = new RequestIdempotentLog();
        createRequestIdempotentLog.setId(id);
        createRequestIdempotentLog.setCreateTime(System.currentTimeMillis());
        requestIdempotentLogRepository.save(createRequestIdempotentLog);
        return createRequestIdempotentLog;
    }

    @Override
    @CachePut(value = "RequestIdempotentLog", key = "{#sportUnsettleData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public RequestIdempotentLog create(SportUnsettleData sportUnsettleData, String vendorPlayerUsername) {
        String id = this.generateUnsettleRequestIdempotentLogId(sportUnsettleData, vendorPlayerUsername);
        RequestIdempotentLog createRequestIdempotentLog = new RequestIdempotentLog();
        createRequestIdempotentLog.setId(id);
        createRequestIdempotentLog.setCreateTime(System.currentTimeMillis());
        requestIdempotentLogRepository.save(createRequestIdempotentLog);
        return createRequestIdempotentLog;
    }

    @Override
    @CachePut(value = "RequestIdempotentLog", key = "{#sportAdjustmentData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager")
    public RequestIdempotentLog create(SportAdjustmentData sportAdjustmentData, String vendorPlayerUsername) {
        String id = this.generateAdjustmentRequestIdempotentLogId(sportAdjustmentData, vendorPlayerUsername);
        RequestIdempotentLog createRequestIdempotentLog = new RequestIdempotentLog();
        createRequestIdempotentLog.setId(id);
        createRequestIdempotentLog.setCreateTime(System.currentTimeMillis());
        requestIdempotentLogRepository.save(createRequestIdempotentLog);
        return createRequestIdempotentLog;
    }

    @Override
    @Cacheable(value = "RequestIdempotentLog", key = "{#sportBetResultData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager", unless = "#result == null")
    public RequestIdempotentLog checkExists(SportBetResultData sportBetResultData, String vendorPlayerUsername)
            throws TransactionStillProcessingException {
        String id = this.generateBetResultRequestIdempotentLogId(sportBetResultData, vendorPlayerUsername);
        return requestIdempotentLogRepository.findById(id).orElse(null);
    }

    @Override
    @Cacheable(value = "RequestIdempotentLog", key = "{#sportRefundData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager", unless = "#result == null")
    public RequestIdempotentLog checkExists(SportRefundData sportRefundData, String vendorPlayerUsername)
            throws TransactionStillProcessingException {
        String id = this.generateRefundRequestIdempotentLogId(sportRefundData, vendorPlayerUsername);
        return requestIdempotentLogRepository.findById(id).orElse(null);
    }

    @Override
    @Cacheable(value = "RequestIdempotentLog", key = "{#sportUnsettleData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager", unless = "#result == null")
    public RequestIdempotentLog checkExists(SportUnsettleData sportUnsettleData, String vendorPlayerUsername)
            throws TransactionStillProcessingException {
        String id = this.generateUnsettleRequestIdempotentLogId(sportUnsettleData, vendorPlayerUsername);
        return requestIdempotentLogRepository.findById(id).orElse(null);
    }

    @Override
    @Cacheable(value = "RequestIdempotentLog", key = "{#sportAdjustmentData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager", unless = "#result == null")
    public RequestIdempotentLog checkExists(SportAdjustmentData sportAdjustmentData, String vendorPlayerUsername)
            throws TransactionStillProcessingException {
        String id = this.generateAdjustmentRequestIdempotentLogId(sportAdjustmentData, vendorPlayerUsername);
        return requestIdempotentLogRepository.findById(id).orElse(null);
    }
}
