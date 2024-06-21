package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.exception.AgentNotFoundException;
import com.nextgen.gameaggregator.repository.ga.writer.AgentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;

    public AgentServiceImpl(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Override
    @Cacheable(value = "Agent", key = "#id", cacheManager = "cacheManager")
    public Agent get(Integer id) throws AgentNotFoundException {
        return agentRepository.findById(id).orElseThrow(AgentNotFoundException::new);
    }
}
