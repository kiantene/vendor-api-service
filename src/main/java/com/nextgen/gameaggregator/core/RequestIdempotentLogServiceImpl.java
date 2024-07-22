package com.nextgen.gameaggregator.core;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
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

        String requestIdempotentLogId = externalTransactionId + "_" + vendorPlayerUsername;
        return requestIdempotentLogId;
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
    @Cacheable(value = "RequestIdempotentLog", key = "{#betResultData.externalTransactionId, #vendorPlayerUsername}", cacheManager = "cacheManager", unless = "#result == null")
    public RequestIdempotentLog checkExists(BetResultData betResultData, String vendorPlayerUsername) throws TransactionStillProcessingException {
        String id = this.generateRequestIdempotentLogId(betResultData, vendorPlayerUsername);
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
}
