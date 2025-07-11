package com.nextgen.gameaggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AgentVendorProxyService {

    @Value("${smart.routing.enabled:false}")
    private Boolean smartRoutingEnabled;

    private final AgentVendorProxyCacheService cacheService;

    public AgentVendorProxyService(AgentVendorProxyCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public String applyProxy(Integer agentId, Integer vendorId, String gameUrl) {
        if (!smartRoutingEnabled) return gameUrl;

        if (gameUrl == null || gameUrl.isEmpty()) return gameUrl;

        List<DomainMapping> mappings = cacheService.getCachedDomainMappings(agentId, vendorId);
        if (mappings.isEmpty()) return gameUrl;

        String lowerUrl = gameUrl.toLowerCase();
        for (DomainMapping mapping : mappings) {
            int index = lowerUrl.indexOf(mapping.vendorDomainLower);
            if (index >= 0) {
                String before = gameUrl.substring(0, index);
                String after = gameUrl.substring(index + mapping.vendorDomain.length());
                return before + mapping.proxyDomain + after;
            }
        }

        return gameUrl;
    }

    public record DomainMapping(String vendorDomain, String vendorDomainLower, String proxyDomain) {
    }
}
