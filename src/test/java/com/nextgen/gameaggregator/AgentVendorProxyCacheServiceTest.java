package com.nextgen.gameaggregator;

import com.nextgen.gameaggregator.entity.ga.AgentVendorProxy;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.repository.ga.reader.AgentVendorProxyRepository;
import com.nextgen.gameaggregator.service.AgentVendorProxyCacheService;
import com.nextgen.gameaggregator.service.AgentVendorProxyService;
import com.nextgen.gameaggregator.service.AgentVendorProxyService.DomainMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentVendorProxyCacheServiceTest {

    private AgentVendorProxyRepository repository;
    private AgentVendorProxyCacheService cacheService;

    @BeforeEach
    void setUp() {
        repository = mock(AgentVendorProxyRepository.class);
        cacheService = new AgentVendorProxyCacheService(repository);
    }

    @Test
    void shouldReturnEmptyList_whenNoProxiesFound() {
        when(repository.findAllByAgentIdAndVendorId(1, 100)).thenReturn(Collections.emptyList());

        List<AgentVendorProxyService.DomainMapping> result = cacheService.getCachedDomainMappings(1, 100);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyList_whenNoActiveProxies() {
        AgentVendorProxy inactive = new AgentVendorProxy();
        inactive.setStatus(Status.INACTIVE.code);
        when(repository.findAllByAgentIdAndVendorId(1, 100)).thenReturn(Collections.singletonList(inactive));

        List<AgentVendorProxyService.DomainMapping> result = cacheService.getCachedDomainMappings(1, 100);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnMappedDomains_whenValidProxiesProvided() {
        AgentVendorProxy proxy = new AgentVendorProxy();
        proxy.setStatus(Status.ACTIVE.code);
        proxy.setVendorDomain("Vendor.COM");
        proxy.setProxyDomain("Proxy.COM");

        when(repository.findAllByAgentIdAndVendorId(1, 100)).thenReturn(Collections.singletonList(proxy));

        List<AgentVendorProxyService.DomainMapping> result = cacheService.getCachedDomainMappings(1, 100);

        assertEquals(1, result.size());
        assertEquals("Vendor.COM", result.get(0).vendorDomain());
        assertEquals("vendor.com", result.get(0).vendorDomainLower());
        assertEquals("Proxy.COM", result.get(0).proxyDomain());

    }

    @Test
    void shouldReturnEmptyList_whenVendorDomainIsInvalid() {
        AgentVendorProxy proxy = new AgentVendorProxy();
        proxy.setStatus(Status.ACTIVE.code);
        proxy.setVendorDomain("  bad.com ");
        proxy.setProxyDomain("proxy.com");

        when(repository.findAllByAgentIdAndVendorId(1, 100)).thenReturn(Collections.singletonList(proxy));

        List<AgentVendorProxyService.DomainMapping> result = cacheService.getCachedDomainMappings(1, 100);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyList_whenProxyDomainIsNull() {
        AgentVendorProxy proxy = new AgentVendorProxy();
        proxy.setStatus(Status.ACTIVE.code);
        proxy.setVendorDomain("vendor.com");
        proxy.setProxyDomain(null);

        when(repository.findAllByAgentIdAndVendorId(1, 100)).thenReturn(Collections.singletonList(proxy));

        List<AgentVendorProxyService.DomainMapping> result = cacheService.getCachedDomainMappings(1, 100);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyList_whenDomainExceedsMaxLength() {
        String longDomain = "a".repeat(254);
        AgentVendorProxy proxy = new AgentVendorProxy();
        proxy.setStatus(Status.ACTIVE.code);
        proxy.setVendorDomain(longDomain);
        proxy.setProxyDomain("proxy.com");

        when(repository.findAllByAgentIdAndVendorId(1, 100)).thenReturn(Collections.singletonList(proxy));

        List<AgentVendorProxyService.DomainMapping> result = cacheService.getCachedDomainMappings(1, 100);

        assertTrue(result.isEmpty());
    }
}
