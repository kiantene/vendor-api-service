package com.nextgen.gameaggregator.config.webclient;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "operator-api.web-client.connection-pool")
public class OperatorApiWebClientPoolProperties {
    private Integer maxConnections;
    private Integer pendingAcquireMaxCount;

    public int getMaxConnectionsOrDefault(int fallback) {
        return maxConnections != null ? maxConnections : fallback;
    }

    public int getPendingAcquireMaxCountOrDefault(int fallback) {
        return pendingAcquireMaxCount != null ? pendingAcquireMaxCount : fallback;
    }
}
