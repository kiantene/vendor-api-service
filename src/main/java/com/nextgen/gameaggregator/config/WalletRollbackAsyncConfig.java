package com.nextgen.gameaggregator.config;

import com.nextgen.gameaggregator.core.concurrent.AppThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class WalletRollbackAsyncConfig {
    @Bean(name = "walletRollbackEventExecutor")
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(500);
        executor.setThreadFactory(AppThreadFactory.named("wallet-rollback-event"));
        executor.initialize();
        return executor;
    }
}
