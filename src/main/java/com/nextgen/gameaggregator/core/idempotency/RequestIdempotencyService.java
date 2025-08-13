package com.nextgen.gameaggregator.core.idempotency;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.repository.ga.writer.RequestIdempotentLogRepository;
import org.springframework.stereotype.Service;

@Service
public class RequestIdempotencyService {

    private final RequestIdempotentLogRepository repository;

    public RequestIdempotencyService(RequestIdempotentLogRepository repository) {
        this.repository = repository;
    }

    // Do not need to cache in Redis as this operation is using KV access which is already performant
    public boolean isDuplicateRequest(String vendorClassName, String action, String idempotencyKey) {
        String docId = key(vendorClassName, action, idempotencyKey);
        if (repository.findById(docId).isPresent()) {
            return true;
        }

        RequestIdempotentLog requestIdempotentLog = new RequestIdempotentLog();
        requestIdempotentLog.setId(docId);
        requestIdempotentLog.setCreateTime(System.currentTimeMillis());
        repository.save(requestIdempotentLog);

        return false;
    }

    public static String key(String vendorClassName, String action, String idempotencyKey) {
        return vendorClassName + "::" + action + "::" + idempotencyKey;
    }
}
