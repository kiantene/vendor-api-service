package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.exception.AgentNotFoundException;

public interface AgentService {
    Agent get(Integer id) throws AgentNotFoundException;
}
