package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentApiCredentialRepository extends JpaRepository<AgentApiCredential, Integer> {
    AgentApiCredential findByApiKey(String apiKey);
    AgentApiCredential findByAgentIdAndStatus(Integer agentId, Integer status);

    /**
     * @deprecated This method is deprecated, Use {@link #findAllByAgentId()} instead.
     */
    @Deprecated
    AgentApiCredential findByAgentId(Integer agentId);

    List<AgentApiCredential> findAllByAgentId(Integer agentId);
}
