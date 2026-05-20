package com.nextgen.gameaggregator.config;

import com.nextgen.gameaggregator.data.kafka.betdetails.DropAndCountRejectionPolicy;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "endRoundExecutor")
    public Executor endRoundExecutor(MeterRegistry registry) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(32);          // number of threads always kept alive
        ex.setMaxPoolSize(128);          // upper cap
        ex.setQueueCapacity(2000);       // backlog before rejecting
        ex.setThreadNamePrefix("endround-");
        ex.setAllowCoreThreadTimeOut(false); // Core threads won't die when idle
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // This slows down the producer, providing back-pressure instead of dropping tasks or throwing exceptions.
        ex.setWaitForTasksToCompleteOnShutdown(true); // graceful shutdown
        ex.setAwaitTerminationSeconds(30); // Maximum time to wait during shutdown for tasks to finish.
        ex.initialize();

        // Micrometer metrics for this executor
        return ExecutorServiceMetrics.monitor(
                registry,
                ex.getThreadPoolExecutor(),
                "endround.executor",
                Tags.of("executor", "endRound")
        );
    }

    /**
     * Async executor for {@code RawSportsBetDetailsProducer.emit}. Pipeline-isolated
     * from the livecasino executor so a stuck topic in one pipeline cannot starve
     * the other.
     *
     * <p>Saturation behaviour is non-blocking by design: {@link DropAndCountRejectionPolicy}
     * silently drops + counts, never throws, never runs the task on the caller. With
     * {@code @Async} on a {@code void} method this means the betting-request thread
     * is never affected by Kafka back-pressure.
     */
    @Bean(name = "rawSportsBetDetailsExecutor")
    public Executor rawSportsBetDetailsExecutor(MeterRegistry registry) {
        // Sized for ~10k QPS of bet-detail emits. Note the JDK ThreadPoolExecutor quirk:
        // with a bounded LinkedBlockingQueue, threads beyond corePoolSize are only spun up
        // once the queue is FULL — so the queue absorbs sub-second bursts and maxPoolSize
        // engages only under sustained pressure. 5000-slot queue = ~0.5s buffering at 10k QPS,
        // small enough that DropAndCountRejectionPolicy fires quickly when something is wrong.
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(8);
        ex.setMaxPoolSize(32);
        ex.setQueueCapacity(5000);
        ex.setThreadNamePrefix("raw-sports-bet-details-emit-");
        ex.setRejectedExecutionHandler(new DropAndCountRejectionPolicy("raw_sports_bet_details", registry));
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(5);
        ex.initialize();

        return ExecutorServiceMetrics.monitor(
                registry,
                ex.getThreadPoolExecutor(),
                "raw_sports_bet_details.executor",
                Tags.of("pipeline", "raw_sports_bet_details")
        );
    }
}
