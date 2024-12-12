package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentPlayerRepository extends JpaRepository<AgentPlayer, Long> {
    AgentPlayer findByAgentIdAndUsername(Integer agentId, String username);

    @Cacheable(value = "AgentPlayers", key = "{#agentId, #username, #status}", cacheManager = "cacheManager")
    AgentPlayer findByAgentIdAndUsernameAndStatus(Integer agentId, String username, Integer status);
}
