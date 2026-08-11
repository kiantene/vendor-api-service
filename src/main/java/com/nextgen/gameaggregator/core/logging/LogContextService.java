package com.nextgen.gameaggregator.core.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.core.context.VendorPlayerAware;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.logging.ApiRequestLog;
import com.nextgen.gameaggregator.service.KafkaService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogContextService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaService kafkaService;

    // api_request_log publishing runs off the caller thread: the Kafka send() can block
    // (max.block.ms, plus the schema-registry HTTP call on a cache miss, which is NOT
    // bounded by max.block.ms). Both callers of logApiRequest() — the request-completion
    // path and WalletRollbackServiceWrapper's fire-and-forget rollback — must never carry
    // that blocking on their own thread. The build (which reads and mutates the LogContext)
    // stays on the caller thread; only the publish is submitted here.
    //
    // The pool is bounded on both dimensions so a Kafka / schema-registry stall degrades
    // logging instead of the service: PUBLISHER_THREADS publisher threads (best-effort audit
    // logging, not a hot path) and a fixed-depth queue. A dropped api_request_log is never
    // silently lost — it is dumped to the local application log on every drop path: the
    // rejection handler (queue saturated, or a task submitted after shutdown) and the
    // drainToLocalLog() sweep of whatever is still queued when @PreDestroy force-stops the pool.
    private static final Gson GSON = new Gson();
    private static final int PUBLISHER_THREADS = 16;
    private static final int PUBLISHER_QUEUE_CAPACITY = 10_000;
    private static final ThreadFactory PUBLISHER_THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger seq = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "api-request-log-publisher-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    };
    private final ThreadPoolExecutor apiRequestLogPublisher = new ThreadPoolExecutor(
            PUBLISHER_THREADS, PUBLISHER_THREADS, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(PUBLISHER_QUEUE_CAPACITY), PUBLISHER_THREAD_FACTORY,
            (task, executor) -> {
                if (task instanceof ApiRequestLogPublishTask publishTask) {
                    publishTask.logLocally("api_request_log publisher queue saturated or shutting down");
                } else {
                    log.warn("Rejected unexpected task type on api_request_log publisher: {}", task.getClass().getName());
                }
            });

    // Carries the built ApiRequestLog so the rejection handler can fall back to a local dump.
    // Submitted via execute() (not submit()) so the executor hands this exact instance — not a
    // FutureTask wrapper — to the rejection handler.
    private final class ApiRequestLogPublishTask implements Runnable {
        private final ApiRequestLog apiRequestLog;

        private ApiRequestLogPublishTask(ApiRequestLog apiRequestLog) {
            this.apiRequestLog = apiRequestLog;
        }

        @Override
        public void run() {
            try {
                publishApiRequestLog(apiRequestLog);
            } catch (Exception e) {
                // outermost safety net: produceApiRequestLog already handles its own failures,
                // so this only fires on an unexpected escape. Keep the stack for debugging AND
                // dump the payload locally, consistent with the rejection handler / drainToLocalLog.
                log.error("Unexpected exception in async ApiRequestLog publisher roundId=[{}]", apiRequestLog.getRoundId(), e);
                logLocally("unexpected exception in async publisher");
            }
        }

        private void logLocally(String reason) {
            log.warn("ApiRequestLog not published ({}), logging locally roundId=[{}]", reason, apiRequestLog.getRoundId());
            log.info(GSON.toJson(apiRequestLog));
        }
    }

    public void debug(String key, Object value) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) return;

        logContext.put(key, value);
    }

    public void log(String key, Object value) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) return;

        logContext.putCustomField(key, value);
    }

    public void logStart(String url, Object body) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) return;

        logContext.setApiStart(System.currentTimeMillis());
        logContext.setApiBody(body);
        logContext.setApiUrl(url);
    }

    public void logEnd(ResponseEntity<String> response) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) return;

        if (logContext.getApiEnd() == 0) {
            logContext.setApiEnd(System.currentTimeMillis());
        }

        if (response == null) return;
        if (logContext.getApiResponse() == null) {
            logContext.setApiResponse(response.getBody());
            logContext.setApiStatusCode(response.getStatusCode().value());
        }
    }

    public static void populateLogContext(LogContext logContext, VendorPlayerAware vendorPlayerAware) {
        if (logContext == null || vendorPlayerAware == null) {
            log.warn("populateLogContext failed: logcontext or vendorPlayerAware is null");
            return;
        }

        logContext.setAgentId(vendorPlayerAware.getAgentId());
        logContext.setVendorId(vendorPlayerAware.getVendorId());
        logContext.setUsername(vendorPlayerAware.getAgentPlayerUsername());
    }

    // Backward compatible function
    public static void updateLogContextFromHttpRequestLog(LogContext logContext, HttpRequestLog httpRequestLog) {
        if (httpRequestLog == null) return;

        if (httpRequestLog.getBetEnd() == null) {
            httpRequestLog.setBetEnd(System.currentTimeMillis());
        }
        if (logContext.getStart() == 0 && httpRequestLog.getBetStart() != null) {
            logContext.setStart(httpRequestLog.getBetStart());
        }
        if (logContext.getEnd() == 0 && httpRequestLog.getBetEnd() != null) {
            logContext.setEnd(httpRequestLog.getBetEnd());
        }
        if (logContext.getApiStart() == 0 && httpRequestLog.getOperatorStart() != null) {
            logContext.setApiStart(httpRequestLog.getOperatorStart());
        }
        if (logContext.getApiEnd() == 0 && httpRequestLog.getOperatorEnd() != null) {
            logContext.setApiEnd(httpRequestLog.getOperatorEnd());
        }
        if (logContext.getApiStatusCode() == null) {
            logContext.setApiStatusCode(httpRequestLog.getOperatorHttpStatusCode());
        }
        logContext.put(HttpRequestLog.class.getSimpleName(), httpRequestLog);
    }

    // Backward compatible function
    public static HttpRequestLog toHttpRequestLog(LogContext logContext) {
        final Integer PROCESSING = 1;

        HttpRequestLog httpRequestLog = new HttpRequestLog();
        logContext.setTraceId(httpRequestLog.getId());
        httpRequestLog.setUrl(logContext.getUrl());
        httpRequestLog.setRequestBody(logContext.getBody() == null ? "" : logContext.getBody().toString());
        httpRequestLog.setBetStart(System.currentTimeMillis());
        httpRequestLog.setStatus(PROCESSING);
        return httpRequestLog;
    }

    public void logApiRequest(LogContext logContext, String responseBody) {
        // build on the caller thread (reads + mutates the LogContext), publish off it
        ApiRequestLog apiRequestLog = buildApiRequestLog(logContext, responseBody);
        if (apiRequestLog != null) {
            apiRequestLogPublisher.execute(new ApiRequestLogPublishTask(apiRequestLog));
        }
    }

    private void publishApiRequestLog(ApiRequestLog apiRequestLog) {
        kafkaService.produceApiRequestLog(apiRequestLog);
    }

    @PreDestroy
    void shutdownApiRequestLogPublisher() {
        apiRequestLogPublisher.shutdown();
        try {
            if (!apiRequestLogPublisher.awaitTermination(10, TimeUnit.SECONDS)) {
                drainToLocalLog(apiRequestLogPublisher.shutdownNow());
            }
        } catch (InterruptedException e) {
            drainToLocalLog(apiRequestLogPublisher.shutdownNow());
            Thread.currentThread().interrupt();
        }
    }

    // shutdownNow() returns the tasks still queued when the pool is force-stopped; dump them
    // locally so a shutdown that coincides with a Kafka/registry stall does not lose them silently
    private void drainToLocalLog(List<Runnable> pending) {
        for (Runnable task : pending) {
            if (task instanceof ApiRequestLogPublishTask publishTask) {
                publishTask.logLocally("api_request_log publisher shut down before publish");
            }
        }
    }

    private ApiRequestLog buildApiRequestLog(LogContext logContext, String responseBody) {
        // This function will only apply to the following request types
        // WalletBalanceAction, WalletBetAction, WalletBetResultAction, WalletRollbackAction
        if (logContext.exists(HttpRequestLog.class.getSimpleName())) {
            HttpRequestLog httpRequestLog = (HttpRequestLog) logContext.get(HttpRequestLog.class.getSimpleName());
            if (httpRequestLog.getRequestType() == null) {
                httpRequestLog.setRequestType(logContext.getLogGroup());
            }
            httpRequestLog.setResponseBody(responseBody);
            httpRequestLog.setEndTime(System.currentTimeMillis());
            httpRequestLog.setOperatorUsername(logContext.getUsername());
            httpRequestLog.setVendorId(logContext.getVendorId());
            httpRequestLog.setAgentId(logContext.getAgentId());

            if (httpRequestLog.getOperatorEndPoints() == null) {
                httpRequestLog.setOperatorEndPoints(logContext.getApiUrl());
            }
            if (httpRequestLog.getOperatorStart() == null && logContext.getApiStart() > 0) {
                httpRequestLog.setOperatorStart(logContext.getApiStart());
            }
            if (httpRequestLog.getOperatorEnd() == null && logContext.getApiEnd() > 0) {
                httpRequestLog.setOperatorEnd(logContext.getApiEnd());
            }
            if (httpRequestLog.getBetStart() == null && logContext.getStart() > 0) {
                httpRequestLog.setBetStart(logContext.getStart());
            }
            if (httpRequestLog.getBetEnd() == null && logContext.getEnd() > 0) {
                httpRequestLog.setBetEnd(logContext.getEnd());
            }

            if (httpRequestLog.getOperatorData() == null) {
                try {
                    httpRequestLog.setOperatorData(objectMapper.writeValueAsString(logContext.getApiBody()));
                    if (logContext.getApiResponse() instanceof String) {
                        httpRequestLog.setOperatorResponse(logContext.getApiResponse().toString());
                    } else {
                        httpRequestLog.setOperatorResponse(objectMapper.writeValueAsString(logContext.getApiResponse()));
                    }
                } catch (Exception ex) {
                    httpRequestLog.setOperatorData(logContext.getApiBody().toString());
                    httpRequestLog.setOperatorResponse(logContext.getApiResponse().toString());
                }
            }

            if (logContext.getException() != null) {
                String exception = logContext.getException();
                httpRequestLog.setStatus(-1);
                httpRequestLog.setErrorMessage(exception);
                httpRequestLog.setExceptionMessage(logContext.getErrorMessage());
                httpRequestLog.setRootCause(logContext.getRootCause());
            } else {
                httpRequestLog.setStatus(2);
            }

            ApiRequestLog apiRequestLog = new ApiRequestLog(httpRequestLog);
            logContext.delete(HttpRequestLog.class.getSimpleName());
            return apiRequestLog;
        }
        return null;
    }
}
