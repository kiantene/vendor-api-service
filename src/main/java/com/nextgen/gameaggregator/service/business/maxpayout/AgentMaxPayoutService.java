package com.nextgen.gameaggregator.service.business.maxpayout;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.enums.Features;
import com.nextgen.gameaggregator.service.data.AgentPayoutSettingDataService;
import com.nextgen.gameaggregator.service.data.VendorFeatureDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgentMaxPayoutService {

    private final AgentDataService agentService;
    private final AgentPayoutSettingDataService payoutSettingsDataService;
    private final VendorFeatureDataService vendorFeatureDataService;

    public BetInformation applyPayoutCap(BetInformation betInfo) {

        if (!vendorFeatureDataService.isVendorEnabled(Features.AGENT_MAX_PAYOUT, betInfo.getVendorId())) {
            return betInfo;
        }

        return getPayoutCapAmount(betInfo)
                .filter(cap -> shouldApplyCap(betInfo, cap))
                .map(cap -> applyCalculation(betInfo, cap))
                .orElse(betInfo);
    }

    private Optional<BigDecimal> getPayoutCapAmount(BetInformation betInfo) {
        Optional<BigDecimal> empty = Optional.empty();

        BigDecimal winAmount = betInfo.getWinAmount();
        BigDecimal jackpotAmount = betInfo.getJackpotAmount();

        if (winAmount == null || winAmount.signum() == 0 || jackpotAmount == null || jackpotAmount.signum() == 0) return empty;

        Optional<Agent> agent = getAgent(betInfo.getAgentId());
        if (agent.isEmpty()) return empty;

        BigDecimal capAmount = payoutSettingsDataService.getMaxPayoutAmount(
                agent.get().getMasterAgentId(),
                agent.get().getId(),
                betInfo.getVendorId(),
                betInfo.getGameCategoryId(),
                betInfo.getCurrencyId()
        );

        if (capAmount == null || capAmount.signum() <= 0) return empty;

        return Optional.of(capAmount);
    }

    private boolean shouldApplyCap(BetInformation betInfo, BigDecimal cap) {
        if (cap == null || betInfo.getWinAmount() == null || betInfo.getJackpotAmount() == null) {
            return false;
        }

        return betInfo.getWinAmount().compareTo(cap) > 0 || betInfo.getJackpotAmount().compareTo(cap) > 0;
    }

    private BetInformation applyCalculation(BetInformation betInfo, BigDecimal cappedAmount) {
        final BigDecimal bet        = normalize(betInfo.getBetAmount());
        final BigDecimal jackpot    = normalize(betInfo.getJackpotAmount());
        final BigDecimal win        = normalize(betInfo.getWinAmount());

        final BigDecimal cappedJackpot = jackpot.min(cappedAmount).max(BigDecimal.ZERO);
        final BigDecimal cappedWin = win.min(cappedAmount).max(BigDecimal.ZERO);
        final BigDecimal cappedWinLoss = cappedWin.subtract(bet);

        betInfo.setUncapWinAmount(betInfo.getWinAmount());
        betInfo.setUncapJackpotAmount(betInfo.getJackpotAmount());
        betInfo.setUncapWinLoss(betInfo.getWinLoss());
        betInfo.setUncapEffectiveTurnover(betInfo.getEffectiveTurnover());

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
        } catch (EntityNotFoundException ex) {
            return Optional.empty();
        }
    }
}
