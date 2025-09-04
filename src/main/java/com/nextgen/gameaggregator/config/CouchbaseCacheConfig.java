package com.nextgen.gameaggregator.config;

import com.couchbase.client.core.retry.FailFastRetryStrategy;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.nextgen.gameaggregator.core.annotation.CacheCollectionFor;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.springframework.beans.factory.annotation.Qualifier;
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

    // TODO : to be remove if this config move to ga-core
    @Bean
    @CacheCollectionFor(entity = Campaign.class)
    public Collection campaignCache(@Qualifier("cacheScope") Scope scope) {
        return scope.collection("campaigns");
    }
}
