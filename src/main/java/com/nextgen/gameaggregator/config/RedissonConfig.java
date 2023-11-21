package com.nextgen.gameaggregator.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedissonConfig {
    private String mode;
    private List<String> nodehosts;
    private String host;
    private String port;
    private String username;
    private String password;
    private Integer database = 0;

    @Bean
    public RedissonClient redissonClient() {
        Config config = createRedissonConfig();
        return Redisson.create(config);
    }

    private Config createRedissonConfig() {
        Config config = new Config();

        if (RedisConfig.RedisMode.CLUSTER.toString().equalsIgnoreCase(mode)) {
            configureCluster(config);
        } else {
            configureSingleServer(config);
        }

        return config;
    }

    private void configureCluster(Config config) {
        ClusterServersConfig clusterConfig = config.useClusterServers();


        nodehosts.forEach(nodeAddress -> {
            clusterConfig.addNodeAddress("redis://" + nodeAddress);
        });

//        List<String> formattedNodeAddresses = nodehosts.stream()
//                .map(nodeAddress -> "redis://" + nodeAddress)
//                .collect(Collectors.toList());
//
//        clusterConfig.setNodeAddresses(formattedNodeAddresses);

        setIfNonEmpty(username, clusterConfig::setUsername);
        setIfNonEmpty(password, clusterConfig::setPassword);
    }

    private void configureSingleServer(Config config) {
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port);

        setIfNonEmpty(username, singleServerConfig::setUsername);
        setIfNonEmpty(password, singleServerConfig::setPassword);
        Optional.ofNullable(database).ifPresent(singleServerConfig::setDatabase);
    }

    private void setIfNonEmpty(String value, Consumer<String> setter) {
        Optional.ofNullable(value)
                .filter(val -> !val.isEmpty()) // Check if the string is non-null and non-empty
                .ifPresent(setter); // Set the value using the setter if it is non-null and non-empty
    }
}

