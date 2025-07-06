package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;

import java.util.List;

public interface AgentApiCredentialService {
    List<AgentApiCredential> getByAgentId(Integer agentId);
    AgentApiCredential getActiveCredential(Integer agentId);
}
