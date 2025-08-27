package com.nextgen.gameaggregator.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "endRoundExecutor")
    public ThreadPoolTaskExecutor endRoundExecutor(MeterRegistry registry) {
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
        ExecutorServiceMetrics.monitor(
                registry,
                ex.getThreadPoolExecutor(),
                "endround.executor",
                Tags.of("executor", "endRound")
        );
        return ex;
    }
}
