package com.nextgen.gameaggregator.core.idempotency;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.repository.ga.writer.RequestIdempotentLogRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class RequestIdempotencyService {
    private static final ThreadLocal<RequestIdempotentLog> currentRequestLog = new ThreadLocal<>();
    private final RequestIdempotentLogRepository repository;

    public RequestIdempotencyService(RequestIdempotentLogRepository repository) {
        this.repository = repository;
    }

    public static void cleanupThreadLocal() {
        currentRequestLog.remove();
    }

    public static String key(String vendorClassName, String action, String idempotencyKey) {
        return vendorClassName + "::" + action + "::" + idempotencyKey;
    }

    // Do not need to cache in Redis as this operation is using KV access which is already performant
    public Optional<RequestIdempotentLog> isDuplicateRequest(String vendorClassName, String action, String idempotencyKey) {
        String docId = key(vendorClassName, action, idempotencyKey);

        Optional<RequestIdempotentLog> existingLog = repository.findById(docId);

        if (existingLog.isPresent()) {
            return existingLog;
        }

        RequestIdempotentLog requestIdempotentLog = new RequestIdempotentLog();
        requestIdempotentLog.setId(docId);
        requestIdempotentLog.setTransactionId(idempotencyKey);
        requestIdempotentLog.setCreateTime(System.currentTimeMillis());
        requestIdempotentLog.setBalance(BigDecimal.ZERO);
        repository.save(requestIdempotentLog);

        currentRequestLog.set(requestIdempotentLog);

        return Optional.empty();
    }

    public void enrichIdempotentLog(String transactionId, String currency, BigDecimal balance) {
        RequestIdempotentLog log = currentRequestLog.get();
        if (log != null) {
            log.setTransactionId(transactionId);
            log.setCurrency(currency);
            log.setBalance(balance);
            repository.save(log);
        }
    }

    public void clearCurrentRequest() {
        RequestIdempotentLog log = currentRequestLog.get();
        if (log != null) {
            repository.deleteById(log.getId());
            currentRequestLog.remove();
        }
    }
}
