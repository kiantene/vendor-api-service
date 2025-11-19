package com.nextgen.gameaggregator.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "walletservice")
public class WalletServiceProperties {
    private String host;
    private Long timeout;

    public String getCallbackUrl() {
        return host + "/seamless";
    }
}
