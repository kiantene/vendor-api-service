package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.AgentVendorLine;
import com.nextgen.gameaggregator.exception.AgentNotFoundException;
import com.nextgen.gameaggregator.exception.CurrencyNotSupportedException;

public interface AgentService {
    Agent get(Integer id) throws AgentNotFoundException;
    Boolean isCurrencySupportedByAgent(Integer agentId, Integer currencyId) throws CurrencyNotSupportedException;
    AgentVendorLine getActiveVendorLine(Integer agentId, Integer vendorId, Integer currencyId, Integer gameCategoryId);
}
