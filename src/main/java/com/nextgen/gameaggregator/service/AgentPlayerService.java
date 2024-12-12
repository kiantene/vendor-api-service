package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AgentPlayerService {

    private final AgentPlayerRepository agentPlayerRepository;
    @Autowired
    public AgentPlayerService(AgentPlayerRepository agentPlayerRepository){
        this.agentPlayerRepository = agentPlayerRepository;
    }

    /**
     * Retrieve an AgentPlayer record based on given Id
     *
     * @param id Id of the AgentPlayer record
     * @return AgentPlayer entity object containing information of the agent's player
     * @throws RecordNotFoundException if no record found
     */
    @Cacheable(value = "AgentPlayers", key = "#id", cacheManager = "cacheManager")
    public AgentPlayer get(Long id) throws RecordNotFoundException {
        return agentPlayerRepository.findById(id).orElseThrow(RecordNotFoundException::new);
    }

    @Cacheable(value = "AgentPlayers", key = "#id", cacheManager = "cacheManager")
    public AgentPlayer verifyAgentPlayerStatus(Long id) throws DisabledAgentPlayerException {
        AgentPlayer agentPlayer;

        try {
            agentPlayer = this.get(id);
        } catch (RecordNotFoundException recordNotFoundException) {
            throw new DisabledAgentPlayerException("PlayerId: " + id);
        }

        if (!agentPlayer.getStatus().equals(Status.ACTIVE.code)) {
            throw new DisabledAgentPlayerException("PlayerId: " + id);
        }

        return agentPlayer;
    }

}
