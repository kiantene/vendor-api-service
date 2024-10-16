package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentCurrency;
import com.nextgen.gameaggregator.exception.CurrencyNotSupportedException;

public interface AgentCurrencyService {
    AgentCurrency getByAgentIdAndCurrencyId(Integer agentId, Integer currencyId) throws CurrencyNotSupportedException;
}
