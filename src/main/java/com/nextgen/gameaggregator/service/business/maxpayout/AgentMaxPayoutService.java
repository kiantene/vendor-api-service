package com.nextgen.gameaggregator.service.business.maxpayout;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.exception.AgentNotFoundException;
import com.nextgen.gameaggregator.service.AgentService;
import com.nextgen.gameaggregator.service.data.VendorPayoutSettingsDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgentMaxPayoutService {

    private final AgentService agentService;
    private final VendorPayoutSettingsDataService payoutSettingsDataService;

    public BetInformation applyPayoutCap(Integer agentId,
                                         Integer vendorId,
                                         Integer currencyId,
                                         BetInformation betInformation) {

        Optional<BigDecimal> payoutCap = this.getPayoutCapAmount(agentId, vendorId, betInformation.getGameCategoryId(), currencyId, betInformation.getWinAmount());

        return payoutCap
                .filter(cap -> shouldApplyCap(betInformation.getWinAmount(), cap))
                .map(cap -> applyCalculation(betInformation, cap))
                .orElse(betInformation);
    }

    private Optional<BigDecimal> getPayoutCapAmount(Integer agentId,
                                                    Integer vendorId,
                                                    Integer gameCategoryId,
                                                    Integer currencyId,
                                                    BigDecimal winAmount) {
        Optional<BigDecimal> empty = Optional.empty();

        if (winAmount == null || winAmount.signum() == 0) return empty;

        Optional<Agent> agent = getAgent(agentId);
        if (agent.isEmpty()) return empty;

        BigDecimal capAmount = payoutSettingsDataService.getMaxPayoutAmount(
                agent.get().getMasterAgentId(),
                agent.get().getId(),
                vendorId,
                gameCategoryId,
                currencyId
        );

        if (capAmount == null || capAmount.signum() <= 0) return empty;

        return Optional.of(capAmount);
    }

    private boolean shouldApplyCap(BigDecimal cap, BigDecimal winAmount) {
        if (cap == null || winAmount == null) {
            return false;
        }
        // only apply if win is strictly greater than cap
        return winAmount.compareTo(cap) > 0;
    }

    private BetInformation applyCalculation(BetInformation betInfo, BigDecimal cappedWin) {
        final BigDecimal bet        = normalize(betInfo.getBetAmount());
        final BigDecimal jackpot    = normalize(betInfo.getJackpotAmount());

        final BigDecimal cappedJackpot = jackpot.min(cappedWin).max(BigDecimal.ZERO);
        final BigDecimal cappedWinLoss = cappedWin.subtract(bet);

        betInfo.setUncapWinAmount(betInfo.getWinAmount());
        betInfo.setUncapWinLoss(betInfo.getWinLoss());
        betInfo.setUncapJackpotAmount(betInfo.getJackpotAmount());

        betInfo.setWinAmount(cappedWin);
        betInfo.setWinLoss(cappedWinLoss);
        betInfo.setJackpotAmount(cappedJackpot);

        return betInfo;
    }

    private static BigDecimal normalize(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Optional<Agent> getAgent(Integer agentId) {
        try {
            return Optional.of(agentService.get(agentId));
        } catch (AgentNotFoundException ex) {
            return Optional.empty();
        }
    }
}
