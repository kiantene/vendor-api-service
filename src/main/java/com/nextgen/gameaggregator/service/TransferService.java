package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.operator.game.url.GameUrlService;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransferService {

    @Autowired
    private AgentPlayerRepository agentPlayerRepository;

    @Autowired
    private GameUrlService gameUrlService;


    @CachePut(value = "AgentPlayers", key = "{#agent.id, #username, #currency.id}" , cacheManager = "cacheManager")
    public AgentPlayer checkAgentPlayer(Agent agent, String username, Currency currency) throws DisabledAgentPlayerException {
        AgentPlayer agentPlayer = agentPlayerRepository.findByAgentIdAndUsername(agent.getId(), username);
        if (agentPlayer == null) {
            agentPlayer = gameUrlService.createAgentPlayer(agent.getId(), username);
        }else{
            if (agentPlayer.getStatus().equals(Status.INACTIVE.code)) {
                throw new DisabledAgentPlayerException();
            }
        }
        return agentPlayer;
    }
}
