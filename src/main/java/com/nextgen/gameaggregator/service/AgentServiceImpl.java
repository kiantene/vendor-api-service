package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.AgentCurrency;
import com.nextgen.gameaggregator.entity.ga.AgentVendorLine;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.AgentNotFoundException;
import com.nextgen.gameaggregator.repository.ga.writer.AgentCurrencyRepository;
import com.nextgen.gameaggregator.repository.ga.writer.AgentRepository;
import com.nextgen.gameaggregator.repository.ga.writer.AgentVendorLineRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;
    private final AgentCurrencyRepository agentCurrencyRepository;
    private final AgentVendorLineRepository agentVendorLineRepository;

    public AgentServiceImpl(AgentRepository agentRepository,
                            AgentCurrencyRepository agentCurrencyRepository,
                            AgentVendorLineRepository agentVendorLineRepository) {

        this.agentRepository = agentRepository;
        this.agentCurrencyRepository = agentCurrencyRepository;
        this.agentVendorLineRepository = agentVendorLineRepository;
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

    @Cacheable(value = "AgentVendorLines", key = "{#agentId, #vendorId, #currencyId, #gameCategoryId}", cacheManager = "cacheManager")
    @Override
    public AgentVendorLine getActiveVendorLine(Integer agentId, Integer vendorId, Integer currencyId, Integer gameCategoryId) {
        List<AgentVendorLine> agentVendorLines = agentVendorLineRepository.
                findByAgentIdAndVendorIdAndCurrencyIdAndGameCategoryId(agentId, vendorId, currencyId, gameCategoryId);

        // vendor line not found
        if (agentVendorLines.isEmpty()) return null;

        AgentVendorLine agentVendorLine = null;
        for (AgentVendorLine line : agentVendorLines) {
            if (line.getStatus().equals(Status.ACTIVE.code)) {
                agentVendorLine = line;
                break;
            }
        }

        return agentVendorLine;
    }
}
