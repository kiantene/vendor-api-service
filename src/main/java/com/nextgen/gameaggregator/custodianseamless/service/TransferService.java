package com.nextgen.gameaggregator.custodianseamless.service;

import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceAccessKeyNotFoundException;
import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.wallet.AccessKey;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.operator.game.url.GameUrlService;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import com.nextgen.gameaggregator.repository.wallet.reader.AccessKeyReaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class TransferService {

    @Autowired
    private AgentPlayerRepository agentPlayerRepository;

    @Autowired
    private GameUrlService gameUrlService;



    @Autowired
    private AccessKeyReaderRepository accessKeyReaderRepository;

    @CachePut(value = "AgentPlayers", key = "{#agent.id, #username}" , cacheManager = "cacheManager")
    public AgentPlayer checkAgentPlayer(Agent agent, String username) throws DisabledAgentPlayerException {
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

    public AccessKey getWalletServiceAccessKey() throws WalletServiceAccessKeyNotFoundException {
        AccessKey accessKey = accessKeyReaderRepository.findFirstByOrderById();
        Optional.ofNullable(accessKey).orElseThrow(WalletServiceAccessKeyNotFoundException::new);
        return accessKey;

    }






}
