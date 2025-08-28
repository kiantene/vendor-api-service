package com.nextgen.gameaggregator.service.maxpayout;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.exception.AgentNotFoundException;
import com.nextgen.gameaggregator.service.AgentService;
import com.nextgen.gameaggregator.service.VendorPayoutSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AgentMaxPayoutService {

    private final AgentService agentService;
    private final VendorPayoutSettingsService vendorPayoutSettingsService;

    public AgentPayout applyPayoutCap(Integer agentId,
                                      Integer vendorId,
                                      Integer currencyId,
                                      BetInformation betInformation) {

        AgentPayout agentPayout = new AgentPayout(betInformation);

        try {
            Agent agent = agentService.get(agentId);
            BigDecimal payoutCap = this.getPayoutCapAmount(agent, vendorId, betInformation.getGameCategoryId(), currencyId);

            if (isZero(betInformation.getWinAmount()) || payoutCap == null) {
                return agentPayout;
            }

            return new AgentPayout(payoutCap, betInformation);

        } catch (AgentNotFoundException e) {
            return agentPayout;
        }

    }

    public BigDecimal getPayoutCapAmount(Agent agent, Integer vendorId, Integer gameCategoryId, Integer currencyId) {
        return vendorPayoutSettingsService.getMaxPayoutAmount(agent.getMasterAgentId(), agent.getId(), vendorId, gameCategoryId, currencyId);

    }

    private boolean isZero(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
}
