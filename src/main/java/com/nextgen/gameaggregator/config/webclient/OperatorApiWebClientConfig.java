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
                .maxConnections(props.getMaxConnectionsOrDefault(500)) // Increase total number of simultaneous open connections (default is 500)
                .pendingAcquireMaxCount(props.getPendingAcquireMaxCountOrDefault(500)) // Increase the number of queued requests waiting for a connection (default is 500)
                .pendingAcquireTimeout(Duration.ofSeconds(10))  // Reduce wait time for a connection before failing (default is 45s)
                .maxIdleTime(Duration.ofSeconds(30))            // Close idle connections after 30s (default is 0s — no idle timeout)
                .maxLifeTime(Duration.ofMinutes(5))             // Close and recycle connections after 5 minutes to avoid staleness (default is 0s — live forever)
                .evictInBackground(Duration.ofSeconds(60))      // Enable periodic background eviction of idle/stale connections (default is 0s — no eviction cycle)
                .metrics(true)                     // Enable actuator metric
                .build();
    }
}
