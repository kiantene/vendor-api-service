package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.AgentPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentPlayerRepository extends JpaRepository<AgentPlayer, Long> {
    AgentPlayer findByAgentIdAndUsername(Integer agentId, String username);

    AgentPlayer findByIdAndStatus(Long id, Integer status);
}
