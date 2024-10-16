package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.AgentCurrency;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentCurrencyRepository extends JpaRepository<AgentCurrency, Integer> {
    @Cacheable(value = "AgentCurrencies", key = "{#agentId, #currencyId, #status}", cacheManager = "cacheManager")
    AgentCurrency findAgentCurrencyByAgentIdAndCurrencyIdAndStatus(Integer agentId, Integer currencyId, Integer status);

    AgentCurrency findByAgentIdAndCurrencyId(Integer agentId, Integer currencyId);

    List<AgentCurrency> findAgentCurrencyByAgentIdAndStatus(Integer agentId,Integer status);

    AgentCurrency findTop1ByAgentIdAndStatus(Integer agentId,Integer status);
}
