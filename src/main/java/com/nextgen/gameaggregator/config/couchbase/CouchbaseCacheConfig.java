package com.nextgen.gameaggregator.config.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.nextgen.gameaggregator.core.annotation.CacheCollectionFor;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.CouchbaseClientFactory;

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

    private final CouchbaseClientFactory factory;

    public CouchbaseCacheConfig(CouchbaseClientFactory factory) {
        this.factory = factory;
    }

    /**
     * Reuse CouchbaseConfig cluster bean instead of creating new one
     */
//    @Bean
//    public ClusterEnvironment cacheClusterEnvironment() {
//        return ClusterEnvironment.builder()
//                .timeoutConfig(timeout -> timeout
//                        .kvTimeout(Duration.ofSeconds(2))
//                        .connectTimeout(Duration.ofSeconds(3))
//                )
//                .retryStrategy(FailFastRetryStrategy.INSTANCE) // No retries, fail instantly
//                .build();
//    }

//    @Bean("gaCluster")
//    public Cluster gaCluster() {
//        return Cluster.connect(connectionString, userName, password);
//    }

    @Bean("gaBucket")
    public Bucket gaBucket() {
        return factory.getBucket();
    }

    // TODO : to be remove if this config move to ga-core
    @Bean
    @CacheCollectionFor(entity = Campaign.class)
    public Collection campaignCache(@Qualifier("cacheScope") Scope scope) {
        return scope.collection("campaigns");
    }

    @Bean("gameScope")
    public Scope gameScope(@Qualifier("gaBucket") Bucket bucket) {
        return bucket.scope("game");
    }

    @Bean
    public Collection gameTransactionsCollection(@Qualifier("gameScope") Scope scope) {
        return scope.collection("game_transactions");
    }

    @Bean
    public Collection gameRoundsCollection(@Qualifier("gameScope") Scope scope) {
        return scope.collection("game_rounds");
    }

    // ---------------- For http retry ----------------
    @Bean("retryScope")
    public Scope retryScope(@Qualifier("gaBucket") Bucket bucket) {
        return bucket.scope("retry");
    }

    @Bean
    public Collection kafkaProducerRetryJobsCollection(@Qualifier("retryScope") Scope scope) {
        return scope.collection("kafka_producer_retry_jobs");
    }

    @Bean
    public Collection httpRetryJobsCollection(@Qualifier("retryScope") Scope scope) {
        return scope.collection("http_retry_jobs");
    }

    // ---------------- For Migration Purpose -----------
    @Bean("migrationScope")
    public Scope migrationScope(@Qualifier("gaBucket") Bucket bucket) {
        return bucket.scope("migration");
    }

    @Bean
    public Collection migrationRoundCollection(@Qualifier("migrationScope") Scope scope) {
        return scope.collection("migration_round");
    }
}
