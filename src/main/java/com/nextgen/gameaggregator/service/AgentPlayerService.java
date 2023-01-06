package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.AgentPlayer;
import com.nextgen.gameaggregator.exception.DisableAgentPlayerException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.repository.AgentPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AgentPlayerService {
    @Autowired
    private AgentPlayerRepository agentPlayerRepository;

    /**
     * Retrieve an AgentPlayer record based on given Id
     *
     * @param id Id of the AgentPlayer record
     * @return AgentPlayer entity object containing information of the agent's player
     * @throws RecordNotFoundException if no record found
     */
    public AgentPlayer get(Long id) throws RecordNotFoundException {
        Optional<AgentPlayer> optional = agentPlayerRepository.findById(id);
        optional.orElseThrow(RecordNotFoundException::new);

        return optional.get();
    }

    public void verifyAgentPlayerStatus(Long id)throws DisableAgentPlayerException {
        AgentPlayer agentPlayer = agentPlayerRepository.findByIdAndStatus(id, 1);
        Optional.ofNullable(agentPlayer).orElseThrow(DisableAgentPlayerException::new);
    }
}
