package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.AgentApiCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentApiCredentialRepository extends JpaRepository<AgentApiCredential, Integer> {
    AgentApiCredential findByApiKey(String apiKey);
    AgentApiCredential findByAgentIdAndStatus(Integer agentId, Integer status);
}
