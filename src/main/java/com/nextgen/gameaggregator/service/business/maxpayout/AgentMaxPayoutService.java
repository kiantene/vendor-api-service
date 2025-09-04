package com.nextgen.gameaggregator.service.business.maxpayout;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.exception.AgentNotFoundException;
import com.nextgen.gameaggregator.service.AgentService;
import com.nextgen.gameaggregator.service.data.VendorPayoutSettingsDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AgentMaxPayoutService {

    private final AgentService agentService;
    private final VendorPayoutSettingsDataService vendorPayoutSettingsDataService;

    public BetInformation applyPayoutCap(Integer agentId,
                                         Integer vendorId,
                                         Integer currencyId,
                                         BetInformation betInformation) {

        try {
            Agent agent = agentService.get(agentId);
            BigDecimal payoutCap = this.getPayoutCapAmount(agent, vendorId, betInformation.getGameCategoryId(), currencyId);

            if (isZero(betInformation.getWinAmount()) || payoutCap == null) {
                return betInformation;
            }

            AgentPayout agentPayout = new AgentPayout(payoutCap, betInformation);

            if (!agentPayout.getCapWinAmount().equals(betInformation.getWinAmount())){
                betInformation.setUncapWinAmount(betInformation.getWinAmount());
                betInformation.setUncapWinLoss(betInformation.getWinLoss());
                betInformation.setUncapJackpotAmount(betInformation.getJackpotAmount());
                betInformation.setUncapEffectiveTurnover(betInformation.getEffectiveTurnover());

                betInformation.setWinAmount(agentPayout.getCapWinAmount());
                betInformation.setWinLoss(agentPayout.getCapWinLoss());
                betInformation.setJackpotAmount(agentPayout.getCapJackpotAmount());
                betInformation.setEffectiveTurnover(agentPayout.getCapEffectiveTurnover());
            }

            return betInformation;

        } catch (AgentNotFoundException e) {
            return betInformation;
        }

    }

    public BigDecimal getPayoutCapAmount(Agent agent, Integer vendorId, Integer gameCategoryId, Integer currencyId) {
        return vendorPayoutSettingsDataService.getMaxPayoutAmount(agent.getMasterAgentId(), agent.getId(), vendorId, gameCategoryId, currencyId);

    }

    private boolean isZero(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
}
