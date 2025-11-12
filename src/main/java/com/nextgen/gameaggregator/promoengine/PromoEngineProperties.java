package com.nextgen.gameaggregator.promoengine;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "promo-engine")
public class PromoEngineProperties {
    private String host;
    private String apiSecret;
}
