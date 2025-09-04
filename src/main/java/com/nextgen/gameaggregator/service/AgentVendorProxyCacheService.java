package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentVendorProxy;
import com.nextgen.gameaggregator.enums.Status;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import com.nextgen.gameaggregator.repository.ga.reader.AgentVendorProxyRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AgentVendorProxyCacheService {

    private static final int MAX_DOMAIN_LENGTH = 253;

    private final AgentVendorProxyRepository repository;

    public AgentVendorProxyCacheService(AgentVendorProxyRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "AgentVendorDomainMappings", key = "{#agentId, #vendorId}", cacheManager ="cacheManager")
    public List<AgentVendorProxyService.DomainMapping> getCachedDomainMappings(Integer agentId, Integer vendorId) {
        List<AgentVendorProxy> proxies = repository.findAllByAgentIdAndVendorId(agentId, vendorId);

        if (proxies == null || proxies.isEmpty()) {
            log.debug("No proxy config for agentId={}, vendorId={}", agentId, vendorId);
            return Collections.emptyList();
        }

        List<AgentVendorProxy> activeProxies = proxies.stream()
                .filter(proxy -> Status.ACTIVE.code.equals(proxy.getStatus()))
                .toList();

        if (activeProxies.isEmpty()) {
            log.debug("No active proxy config for agentId={}, vendorId={}", agentId, vendorId);
            return Collections.emptyList();
        }

        List<AgentVendorProxyService.DomainMapping> mappings = new ArrayList<>();
        for (AgentVendorProxy proxy : activeProxies) {
            String vendorDomain = proxy.getVendorDomain();
            String proxyDomain = proxy.getProxyDomain();

            if (isDomainInvalid(vendorDomain, "vendor domain")) {
                return Collections.emptyList();
            }

            if (isDomainInvalid(proxyDomain, "proxy domain")) {
                return Collections.emptyList();
            }

            mappings.add(new AgentVendorProxyService.DomainMapping(vendorDomain, vendorDomain.toLowerCase(), proxyDomain));
        }

        return mappings;
    }

    private boolean isDomainInvalid(String domain, String fieldName) {
        if (domain == null || domain.trim().isEmpty()) {
            log.warn("Invalid proxy config for domain={}, fieldName={}, domain is null or empty", domain, fieldName);
            return true;
        }

        if (!domain.equals(domain.trim())) {
            log.warn("Invalid proxy config for domain={}, fieldName={}, has leading or trailing whitespace", domain, fieldName);
            return true;
        }

        if (domain.length() > MAX_DOMAIN_LENGTH) {
            log.warn("Invalid proxy config for domain={}, fieldName={}, exceeds max length of {}", domain, fieldName, MAX_DOMAIN_LENGTH);
            return true;
        }

        return false;
    }
}
