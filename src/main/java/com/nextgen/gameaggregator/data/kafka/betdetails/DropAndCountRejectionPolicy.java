package com.nextgen.gameaggregator.data.kafka.betdetails;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link RejectedExecutionHandler} for bet-detail emit executors. When the
 * executor's queue is saturated the rejected task is <strong>silently dropped,
 * counted, and warn-logged (rate-limited)</strong> — it is not run on the
 * caller thread and no exception is propagated.
 *
 * <p>This non-throwing behavior is the load-bearing property. Bet-detail emit
 * is wired with {@code @Async} on a {@code void} method; if this handler
 * threw {@link java.util.concurrent.RejectedExecutionException} (as the JDK's
 * default {@code AbortPolicy} does) Spring's async interceptor would propagate
 * it back to the caller's thread — i.e. the betting-request thread. Dropping
 * a raw bet-detail event under sustained back-pressure is acceptable; blocking
 * or failing a player's bet is not.
 *
 * <p>Drops are still observable:
 * <ul>
 *   <li>Counter {@code ga.bet_details.emit.submit{pipeline,outcome=rejected}}
 *       increments on every drop.</li>
 *   <li>A {@code WARN}-level log fires on the first drop and at most once per
 *       {@code LOG_THROTTLE_MS} thereafter, to avoid flooding logs during an
 *       outage.</li>
 * </ul>
 */
@Slf4j
public final class DropAndCountRejectionPolicy implements RejectedExecutionHandler {

    private static final long LOG_THROTTLE_MS = 10_000L;

    private final String pipeline;
    private final Counter rejectedCounter;
    private final AtomicLong lastLogAtMs = new AtomicLong();

    public DropAndCountRejectionPolicy(String pipeline, MeterRegistry meterRegistry) {
        this.pipeline = pipeline;
        this.rejectedCounter = Counter.builder("ga.bet_details.emit.submit")
                .description("Bet-detail emit tasks rejected by the async executor (dropped, not blocked)")
                .tag("pipeline", pipeline)
                .tag("outcome", "rejected")
                .register(meterRegistry);
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        rejectedCounter.increment();
        long now = System.currentTimeMillis();
        long last = lastLogAtMs.get();
        if (now - last >= LOG_THROTTLE_MS && lastLogAtMs.compareAndSet(last, now)) {
            log.warn("Dropping bet-detail emit task: queue saturated pipeline={} queueSize={} (further drops suppressed for {}ms)",
                    pipeline, executor.getQueue().size(), LOG_THROTTLE_MS);
        }
    }
}
