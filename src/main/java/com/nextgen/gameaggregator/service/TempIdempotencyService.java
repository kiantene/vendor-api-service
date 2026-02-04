package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.TempIdempotency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TempIdempotencyService {

    private static final int MAX_COUNT = 3;
    private final CachingService cachingService;

    public TempIdempotencyService(CachingService cachingService) {
        this.cachingService = cachingService;
    }

    public TempIdempotency generate(String idempotencyKey) {
        return TempIdempotency.ofNew(idempotencyKey);
    }

    public boolean isIdempotencyMaxCount(String idempotencyKey) {
        TempIdempotency existing = cachingService.get(idempotencyKey);

        // first time
        if (existing == null) {
            cachingService.put(TempIdempotency.ofNew(idempotencyKey));
            return false;
        }

        // increment
        int newCount = existing.getCount() + 1;
        existing.setCount(newCount);

        if (newCount > MAX_COUNT) {
            cachingService.evict(idempotencyKey);
            return true;
        }

        cachingService.put(existing);
        return false;
    }
}
