package com.nextgen.gameaggregator.config.webclient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OperatorApiWebClientPoolProperties.class)
public class OperatorApiWebClientConfig {

    @Bean
    public ConnectionProvider operatorApiConnectionProvider(OperatorApiWebClientPoolProperties props) {
        return ConnectionProvider.builder("operator-api-web-client-pool")
                .maxConnections(props.getMaxConnections())
                .pendingAcquireMaxCount(props.getPendingAcquireMaxCount())
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .evictInBackground(Duration.ofSeconds(60))
                .build();
    }
}
