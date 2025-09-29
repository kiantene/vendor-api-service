package com.nextgen.gameaggregator.core.retry.couchbase;

import com.couchbase.client.core.error.DocumentExistsException;
import com.nextgen.gameaggregator.core.retry.HttpCallSpec;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.retry.enums.RetryOrigin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class CouchbaseRetryQueueService implements RetryQueueService {
    private final CouchbaseHttpRetryJobDataService dataService;

    public CouchbaseRetryQueueService(CouchbaseHttpRetryJobDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public Mono<Void> enqueue(HttpCallSpec spec, RetryOrigin origin) {
        final HttpRetryJob job = toJob(spec, origin);

        return Mono.fromRunnable(() -> {
                    try {
                        dataService.insert(job);
                    } catch (DocumentExistsException ignored) {
                        // idempotent: already enqueued → treat as success
                    }
                })
                .doOnError(e -> logError(job, e))
                .then();
    }

    private static HttpRetryJob toJob(HttpCallSpec spec, RetryOrigin origin) {
        HttpRetryJob job = new HttpRetryJob();
        job.setId(spec.getTraceId());
        job.setTraceId(spec.getTraceId());
        job.setOrigin(origin.name());
        job.setPartition(spec.getPartition());
        job.setPartitionKey(spec.getPartitionKey());
        job.setAgentId(spec.getAgentId());
        job.setMethod(spec.getMethod());
        job.setUrl(spec.getUrl());
        job.setHeaders(spec.getHeaders());
        job.setBodyJson(spec.getBodyJson());
        job.setTransactionTime(spec.getTransactionTime());
        return job;
    }

    private static void logError(HttpRetryJob job, Throwable e) {
        log.error("Failed to enqueue retry job id={} url={} body={}", job.getId(), job.getUrl(), job.getBodyJson(), e);
    }
}
