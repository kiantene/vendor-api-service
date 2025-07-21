package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentApiCredentialDataService {

    private final AgentApiCredentialCacheService cache;

    public AgentApiCredential getActiveCredential(Integer agentId) {
        return cache.getActiveCredential(agentId);
    }
}
