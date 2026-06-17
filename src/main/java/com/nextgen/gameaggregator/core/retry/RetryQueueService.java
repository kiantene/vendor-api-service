package com.nextgen.gameaggregator.core.retry;

import com.nextgen.gameaggregator.core.retry.enums.RetryOrigin;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface RetryQueueService {
    Mono<Void> enqueue(HttpCallSpec spec, RetryOrigin origin);
    Mono<Void> enqueueWithDelay(HttpCallSpec spec, RetryOrigin origin, Duration delay);
}
