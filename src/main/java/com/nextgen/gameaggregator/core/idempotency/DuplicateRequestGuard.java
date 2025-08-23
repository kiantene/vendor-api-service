package com.nextgen.gameaggregator.core.idempotency;

import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import org.springframework.stereotype.Service;

@Service
public class DuplicateRequestGuard {

    private final RequestIdempotencyService service;

    public DuplicateRequestGuard(RequestIdempotencyService service) {
        this.service = service;
    }

    public void ensureNotDuplicate(String vendor, String action, String key) {
        if (service.isDuplicateRequest(vendor, action, key)) {
            throw new DuplicateRequestException(RequestIdempotencyService.key(vendor, action, key) + " already processed");
        }
    }

    public void clear() {
        service.clearCurrentRequest();
    }

    public void cleanup() {
        RequestIdempotencyService.cleanupThreadLocal();
    }
}
