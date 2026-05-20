package com.nextgen.gameaggregator.data.kafka.betdetails;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DropAndCountRejectionPolicyTest {

    private final MeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void rejectionIsCountedAndDoesNotThrow() throws Exception {
        // One worker, queue of size one. Block the worker, fill the queue, then submit again
        // — the third submission would normally throw with AbortPolicy. With this policy it must
        // be silently dropped (no exception) and counted.
        DropAndCountRejectionPolicy policy = new DropAndCountRejectionPolicy("test", registry);
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1), policy);
        try {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger ran = new AtomicInteger();

            tpe.execute(() -> { ran.incrementAndGet(); started.countDown(); awaitQuietly(release); });
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

            tpe.execute(ran::incrementAndGet);                  // queued
            assertThatCode(() -> tpe.execute(ran::incrementAndGet)).doesNotThrowAnyException(); // dropped
            assertThatCode(() -> tpe.execute(ran::incrementAndGet)).doesNotThrowAnyException(); // dropped

            assertThat(rejectedCount()).isEqualTo(2.0);

            release.countDown();
        } finally {
            tpe.shutdown();
            tpe.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void rejectionCounterIsTaggedByPipeline() {
        new DropAndCountRejectionPolicy("raw_sports_bet_details", registry);
        new DropAndCountRejectionPolicy("raw_bet_details", registry);

        // Both counters are registered at construction time so dashboards can target each pipeline.
        assertThat(registry.find("ga.bet_details.emit.submit")
                .tag("pipeline", "raw_sports_bet_details").tag("outcome", "rejected").counter()).isNotNull();
        assertThat(registry.find("ga.bet_details.emit.submit")
                .tag("pipeline", "raw_bet_details").tag("outcome", "rejected").counter()).isNotNull();
    }

    private double rejectedCount() {
        return registry.counter("ga.bet_details.emit.submit", "pipeline", "test", "outcome", "rejected").count();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
