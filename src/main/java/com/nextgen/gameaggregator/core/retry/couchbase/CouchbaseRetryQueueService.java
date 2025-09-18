package com.nextgen.gameaggregator.core.retry.couchbase;

import com.couchbase.client.core.error.DocumentExistsException;
import com.nextgen.gameaggregator.core.retry.HttpCallSpec;
import com.nextgen.gameaggregator.core.retry.RetryOrigin;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

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
        job.setId(buildId(spec, origin));
        job.setTraceId(spec.getTraceId());
        job.setOrigin(origin.name());
        job.setMethod(spec.getMethod());
        job.setUrl(spec.getUrl());
        job.setHeaders(spec.getHeaders());
        job.setBodyJson(spec.getBodyJson());
        return job;
    }

    private static String buildId(HttpCallSpec spec, RetryOrigin origin) {
        String raw = spec.getMethod() + "|" +
                spec.getUrl() + "|" +
                spec.getIdempotencyKey() + "|" +
                (spec.getBodyJson() == null ? "" : spec.getBodyJson());
        String hash = sha256Hex(raw);
        return origin + "::" + hash;
    }

    private static void logError(HttpRetryJob job, Throwable e) {
        log.error("Failed to enqueue retry job id={} url={} body={}", job.getId(), job.getUrl(), job.getBodyJson(), e);
    }
}
