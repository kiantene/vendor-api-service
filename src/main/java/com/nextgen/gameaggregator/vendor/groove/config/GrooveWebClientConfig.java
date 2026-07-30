package com.nextgen.gameaggregator.vendor.groove.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GrooveWebClientConfig {

    @Bean("grooveWebClient")
    public WebClient grooveWebClient() {
        return WebClient.builder().build();
    }
}