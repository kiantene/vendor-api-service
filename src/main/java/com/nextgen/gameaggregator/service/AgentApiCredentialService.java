package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.repository.AgentApiCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgentApiCredentialService {
    @Autowired
    private AgentApiCredentialRepository agentApiCredentialRepository;

    public String getCallbackUrl(Integer agentId) {
        // TODO: error handling
        final Integer STATUS_ACTIVE = 1; // TODO: to refactor
        AgentApiCredential credential = agentApiCredentialRepository.findByAgentIdAndStatus(agentId, STATUS_ACTIVE);
        return credential.getCallbackUrl();
    }
}
