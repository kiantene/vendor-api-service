package com.nextgen.gameaggregator.vendor.saba.service;

import io.undertow.conduits.GzipStreamSourceConduit;
import io.undertow.server.handlers.encoding.RequestEncodingHandler;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.context.annotation.Bean;

public class GzipService {
    @Bean
    public UndertowServletWebServerFactory undertowServletWebServerFactory() {
        UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
        factory.addDeploymentInfoCustomizers((deploymentInfo) -> {
            deploymentInfo.addInitialHandlerChainWrapper(handler -> new RequestEncodingHandler(handler)
                    .addEncoding("gzip", GzipStreamSourceConduit.WRAPPER));
        });
        return factory;
    }
}
