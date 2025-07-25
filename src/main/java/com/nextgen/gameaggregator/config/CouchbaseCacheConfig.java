package com.nextgen.gameaggregator.config;

import com.couchbase.client.core.retry.FailFastRetryStrategy;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.env.ClusterEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CouchbaseCacheConfig {

    @Value("${spring.couchbase.connectionString}")
    private String connectionString;

    @Value("${spring.couchbase.userName}")
    private String userName;

    @Value("${spring.couchbase.password}")
    private String password;

    @Bean
    public ClusterEnvironment cacheClusterEnvironment() {
        return ClusterEnvironment.builder()
                .timeoutConfig(timeout -> timeout
                        .kvTimeout(Duration.ofSeconds(2))
                        .connectTimeout(Duration.ofSeconds(3))
                )
                .retryStrategy(FailFastRetryStrategy.INSTANCE) // No retries, fail instantly
                .build();
    }

    @Bean("cacheCluster")
    public Cluster cacheCluster() {
        return Cluster.connect(connectionString, userName, password);
    }
}
