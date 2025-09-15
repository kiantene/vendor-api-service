package com.nextgen.gameaggregator.core.retry;

import reactor.core.publisher.Mono;

public interface RetryQueueService {
    Mono<Void> enqueue(HttpCallSpec spec);
}
