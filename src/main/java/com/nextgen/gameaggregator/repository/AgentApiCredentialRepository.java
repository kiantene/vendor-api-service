package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.AgentApiCredential;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentApiCredentialRepository extends JpaRepository<AgentApiCredential, Integer> {
    AgentApiCredential findByApiKey(String apiKey);
    AgentApiCredential findByAgentIdAndStatus(Integer agentId, Integer status);
    AgentApiCredential findByAgentId(Integer agentId);
}
