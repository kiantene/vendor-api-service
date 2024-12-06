package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.AgentCurrency;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.AgentNotFoundException;
import com.nextgen.gameaggregator.repository.ga.writer.AgentCurrencyRepository;
import com.nextgen.gameaggregator.repository.ga.writer.AgentRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;
    private final AgentCurrencyRepository agentCurrencyRepository;

    public AgentServiceImpl(AgentRepository agentRepository,
                            AgentCurrencyRepository agentCurrencyRepository) {

        this.agentRepository = agentRepository;
        this.agentCurrencyRepository = agentCurrencyRepository;
    }

    @Override
    @Cacheable(value = "Agent", key = "#id", cacheManager = "cacheManager")
    public Agent get(Integer id) throws AgentNotFoundException {
        return agentRepository.findById(id).orElseThrow(AgentNotFoundException::new);
    }

    @Override
    @Cacheable(value = "AgentSupportedCurrency", key = "{#agentId, #currencyId}", cacheManager = "cacheManager")
    public Boolean isCurrencySupportedByAgent(Integer agentId, Integer currencyId) {
        AgentCurrency agentCurrency = agentCurrencyRepository.findByAgentIdAndCurrencyId(agentId, currencyId);
        return agentCurrency != null && agentCurrency.getStatus().equals(Status.ACTIVE.code);
    }
}
