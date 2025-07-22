package com.nextgen.gameaggregator.config;

import com.couchbase.client.core.retry.FailFastRetryStrategy;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.env.ClusterEnvironment;
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

    @Value("${spring.couchbase.bucketName}")
    private String bucketName;

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

    @Bean
    @Qualifier("cacheCluster")
    public Cluster cacheCluster() {
        return Cluster.connect(connectionString, userName, password);
    }

    @Bean
    @Qualifier("cacheBucket")
    public Bucket cacheBucket(@Qualifier("cacheCluster") Cluster cluster) {
        return cluster.bucket(bucketName);
    }

    @Bean
    @Qualifier("cacheScope")
    public Scope cacheScope(@Qualifier("cacheBucket") Bucket bucket) {
        return bucket.scope("cache");
    }

    @Bean
    @Qualifier("agentPlayerCollection")
    public Collection agentPlayerCollection(@Qualifier("cacheScope") Scope scope) {
        return scope.collection("agent_players");
    }

    @Bean
    @Qualifier("agentApiCredentialCollection")
    public Collection agentApiCredentialCollection(@Qualifier("cacheScope") Scope scope) {
        return scope.collection("agent_api_credentials");
    }

    @Bean
    @Qualifier("vendorPlayerCollection")
    public Collection vendorPlayerCollection(@Qualifier("cacheScope") Scope scope) {
        return scope.collection("vendor_players");
    }
}
