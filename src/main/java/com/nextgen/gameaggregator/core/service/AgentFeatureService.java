package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.AgentFeature;
import com.nextgen.gameaggregator.enums.Features;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AgentFeatureService {

    private final AgentDataService agentDataService;
    private final AgentFeaturesDataService agentFeaturesDataService;

    public Integer getStatus(Integer agentId, Features feature) {
        Agent agent = agentDataService.get(agentId);

        Integer masterAgentId = agent.getMasterAgentId();

        Optional<AgentFeature> agentFeatureOpt = agentFeaturesDataService.getByMasterAgentIdAndAgentIdAndFeatureId(masterAgentId, agentId, feature.id);

        if (agentFeatureOpt.isPresent()) {
            return agentFeatureOpt.get().getStatus();
        }

        if (agentId > 0) { // Fallback to AgentId = 0
            agentFeatureOpt = agentFeaturesDataService.getByMasterAgentIdAndAgentIdAndFeatureId(masterAgentId, 0, feature.id);
        }

        return agentFeatureOpt.map(AgentFeature::getStatus).orElse(0);
    }
}
