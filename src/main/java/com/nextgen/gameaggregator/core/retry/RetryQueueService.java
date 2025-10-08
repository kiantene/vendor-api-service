package com.nextgen.gameaggregator.core.retry;

import com.nextgen.gameaggregator.core.retry.enums.RetryOrigin;
import reactor.core.publisher.Mono;

public interface RetryQueueService {
    Mono<Void> enqueue(HttpCallSpec spec, RetryOrigin origin);
}
