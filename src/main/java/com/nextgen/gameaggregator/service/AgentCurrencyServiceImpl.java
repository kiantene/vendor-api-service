package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentCurrency;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.CurrencyNotSupportedException;
import com.nextgen.gameaggregator.repository.ga.writer.AgentCurrencyRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AgentCurrencyServiceImpl implements AgentCurrencyService {

    private final AgentCurrencyRepository agentCurrencyRepository;

    public AgentCurrencyServiceImpl(AgentCurrencyRepository agentCurrencyRepository) {
        this.agentCurrencyRepository = agentCurrencyRepository;
    }

    @Cacheable(value = "AgentCurrency", key = "{#agentId, #currencyId}", cacheManager = "cacheManager")
    @Override
    public AgentCurrency getByAgentIdAndCurrencyId(Integer agentId, Integer currencyId) throws CurrencyNotSupportedException {

        AgentCurrency agentCurrency = agentCurrencyRepository.findByAgentIdAndCurrencyId(agentId, currencyId);

        if (agentCurrency == null || agentCurrency.getStatus().equals(Status.INACTIVE.code)) throw new CurrencyNotSupportedException();

        return agentCurrency;
    }
}
