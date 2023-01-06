package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.exception.DisableAgentException;
import com.nextgen.gameaggregator.repository.AgentApiCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    public void verifyAgentStatus(Integer id)throws DisableAgentException {
        AgentApiCredential agentApiCredential = agentApiCredentialRepository.findByAgentIdAndStatus(id, 1);
        Optional.ofNullable(agentApiCredential).orElseThrow(DisableAgentException::new);
    }
}
