package com.nextgen.gameaggregator;

import com.nextgen.gameaggregator.entity.ga.AgentVendorProxy;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.repository.ga.reader.AgentVendorProxyRepository;
import com.nextgen.gameaggregator.service.AgentVendorProxyCacheService;
import com.nextgen.gameaggregator.service.AgentVendorProxyService;
import com.nextgen.gameaggregator.service.AgentVendorProxyService.DomainMapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AgentVendorProxyServiceTest {

    private AgentVendorProxyRepository repository;
    private AgentVendorProxyCacheService cacheService;
    private AgentVendorProxyService service;

    @BeforeEach
    void setUp() {
        repository = mock(AgentVendorProxyRepository.class);
        cacheService = new AgentVendorProxyCacheService(repository);
        service = new AgentVendorProxyService(cacheService);
    }

    @Test
    void applyProxy_shouldReplaceUrlWhenMatchFound() {
        setSmartRoutingEnabled(service, true);

        AgentVendorProxy proxy = buildProxy("original.com", "proxy.com", Status.ACTIVE.code);
        when(repository.findAllByAgentIdAndVendorId(1, 100)).thenReturn(List.of(proxy));

        String result = service.applyProxy(1, 100, "https://original.com/play");
        assertThat(result).isEqualTo("https://proxy.com/play");
    }

    @Test
    void applyProxy_shouldReturnOriginalUrlWhenNoMatchFound() {
        setSmartRoutingEnabled(service, true);

        AgentVendorProxy proxy = buildProxy("example.com", "proxy.com", Status.ACTIVE.code);
        when(repository.findAllByAgentIdAndVendorId(2, 200)).thenReturn(List.of(proxy));

        String result = service.applyProxy(2, 200, "https://nomatch.com/play");
        assertThat(result).isEqualTo("https://nomatch.com/play");
    }

    @Test
    void applyProxy_shouldReturnOriginalUrlIfUrlIsNullOrEmpty() {
        setSmartRoutingEnabled(service, true);

        assertThat(service.applyProxy(1, 1, null)).isNull();
        assertThat(service.applyProxy(1, 1, "")).isEmpty();
    }

    @Test
    void applyProxy_shouldMatchCaseInsensitive() {
        setSmartRoutingEnabled(service, true);

        AgentVendorProxy proxy = buildProxy("Original.COM", "proxy.com", Status.ACTIVE.code);
        when(repository.findAllByAgentIdAndVendorId(10, 20)).thenReturn(List.of(proxy));

        String result = service.applyProxy(10, 20, "https://ORIGINAL.com/play");
        assertThat(result).isEqualTo("https://proxy.com/play");
    }

    @Test
    void applyProxy_shouldUseFirstMatchingDomain() {
        setSmartRoutingEnabled(service, true);

        AgentVendorProxy proxy1 = buildProxy("match.com", "proxy1.com", Status.ACTIVE.code);
        AgentVendorProxy proxy2 = buildProxy("match.com", "proxy2.com", Status.ACTIVE.code);
        when(repository.findAllByAgentIdAndVendorId(12, 22)).thenReturn(List.of(proxy1, proxy2));

        String result = service.applyProxy(12, 22, "https://match.com/game");
        assertThat(result).isEqualTo("https://proxy1.com/game");
    }

    @Test
    void applyProxy_shouldHandleSpecialCharactersInDomain() {
        setSmartRoutingEnabled(service, true);

        AgentVendorProxy proxy = buildProxy("game-original.com", "secure-proxy.com", Status.ACTIVE.code);
        when(repository.findAllByAgentIdAndVendorId(13, 23)).thenReturn(List.of(proxy));

        String result = service.applyProxy(13, 23, "https://game-original.com/start");
        assertThat(result).isEqualTo("https://secure-proxy.com/start");
    }

    @Test
    void applyProxy_shouldSkipInactiveProxies() {
        setSmartRoutingEnabled(service, true);

        AgentVendorProxy proxy = buildProxy("inactive.com", "proxy.com", Status.INACTIVE.code);
        when(repository.findAllByAgentIdAndVendorId(1, 2)).thenReturn(List.of(proxy));

        String result = service.applyProxy(1, 2, "https://inactive.com/play");
        assertThat(result).isEqualTo("https://inactive.com/play");
    }

    @Test
    void applyProxy_shouldReturnOriginalWhenSmartRoutingDisabled() {
        setSmartRoutingEnabled(service, false);

        AgentVendorProxy proxy = buildProxy("original.com", "proxy.com", Status.ACTIVE.code);
        when(repository.findAllByAgentIdAndVendorId(1, 100)).thenReturn(List.of(proxy));

        String result = service.applyProxy(1, 100, "https://original.com/play");
        assertThat(result).isEqualTo("https://original.com/play"); // no replacement
    }

    @Test
    void applyProxy_shouldReturnOriginalIfNoMappingsAndSmartRoutingDisabled() {
        setSmartRoutingEnabled(service, false);

        when(repository.findAllByAgentIdAndVendorId(1, 100)).thenReturn(Collections.emptyList());

        String result = service.applyProxy(1, 100, "https://some.com/path");
        assertThat(result).isEqualTo("https://some.com/path");
    }

    @Test
    void getCachedDomainMappings_shouldFilterAndLowercase() {
        AgentVendorProxy proxy = buildProxy("Test.com", "Proxy.com", Status.ACTIVE.code);
        when(repository.findAllByAgentIdAndVendorId(3, 3)).thenReturn(List.of(proxy));

        List<DomainMapping> mappings = cacheService.getCachedDomainMappings(3, 3);

        assertThat(mappings).hasSize(1);
        assertThat(mappings.get(0).vendorDomain()).isEqualTo("Test.com");
        assertThat(mappings.get(0).vendorDomainLower()).isEqualTo("test.com");
        assertThat(mappings.get(0).proxyDomain()).isEqualTo("Proxy.com");
    }

    @Test
    void getCachedDomainMappings_shouldReturnEmptyIfVendorDomainInvalid() {
        AgentVendorProxy proxy = buildProxy("  bad.com  ", "proxy.com", Status.ACTIVE.code);
        when(repository.findAllByAgentIdAndVendorId(4, 4)).thenReturn(List.of(proxy));

        List<DomainMapping> mappings = cacheService.getCachedDomainMappings(4, 4);
        assertThat(mappings).isEmpty();
    }

    @Test
    void getCachedDomainMappings_shouldReturnEmptyIfProxyDomainInvalid() {
        AgentVendorProxy proxy = buildProxy("valid.com", " proxy.com ", Status.ACTIVE.code);
        when(repository.findAllByAgentIdAndVendorId(5, 5)).thenReturn(List.of(proxy));

        List<DomainMapping> mappings = cacheService.getCachedDomainMappings(5, 5);
        assertThat(mappings).isEmpty();
    }

    @Test
    void getCachedDomainMappings_shouldReturnEmptyIfRepositoryReturnsNullOrEmpty() {
        when(repository.findAllByAgentIdAndVendorId(6, 6)).thenReturn(null);
        assertThat(cacheService.getCachedDomainMappings(6, 6)).isEmpty();

        when(repository.findAllByAgentIdAndVendorId(7, 7)).thenReturn(Collections.emptyList());
        assertThat(cacheService.getCachedDomainMappings(7, 7)).isEmpty();
    }

    private AgentVendorProxy buildProxy(String vendorDomain, String proxyDomain, Integer status) {
        AgentVendorProxy proxy = new AgentVendorProxy();
        proxy.setVendorDomain(vendorDomain);
        proxy.setProxyDomain(proxyDomain);
        proxy.setStatus(status);
        return proxy;
    }

    private void setSmartRoutingEnabled(AgentVendorProxyService service, boolean value) {
        try {
            Field field = AgentVendorProxyService.class.getDeclaredField("smartRoutingEnabled");
            field.setAccessible(true);
            field.set(service, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject smartRoutingEnabled via reflection", e);
        }
    }
}
