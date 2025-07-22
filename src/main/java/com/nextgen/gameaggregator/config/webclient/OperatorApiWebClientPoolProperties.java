package com.nextgen.gameaggregator.config.webclient;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "operator-api.web-client.connection-pool")
@Getter
@Setter
public class OperatorApiWebClientPoolProperties {
    private int maxConnections;
    private int pendingAcquireMaxCount;
}
