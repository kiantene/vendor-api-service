package com.nextgen.gameaggregator.service.business.maxpayout;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.enums.Features;
import com.nextgen.gameaggregator.service.data.AgentPayoutSettingDataService;
import com.nextgen.gameaggregator.service.data.VendorFeatureDataService;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
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

    private static BigDecimal normalize(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ---------------------------------------------------------------------
    // Core: framework-agnostic, no BetInformation. Usable by the new engine.
    // ---------------------------------------------------------------------

    /**
     * Apply the agent max-payout cap to raw vendor-unit amounts.
     *
     * <p>No-ops (returns {@link ResultAmounts#uncapped}) when the vendor is not enabled for
     * {@link Features#AGENT_MAX_PAYOUT}, no cap config exists, or the amounts are within the cap.
     *
     * @param toVendorRate rate to convert the operator-currency cap config into vendor units,
     *                     so the comparison happens in the same space as {@link CapRequest} amounts.
     */
    public ResultAmounts applyPayoutCap(CapRequest req, BigDecimal toVendorRate) {
        if (!vendorFeatureDataService.isVendorEnabled(Features.AGENT_MAX_PAYOUT, req.vendorId())) {
            return ResultAmounts.uncapped(req.betAmount(), req.winAmount(), req.jackpotAmount());
        }

        return resolveCapAmount(req, toVendorRate)
                .filter(cap -> exceedsCap(req, cap))
                .map(cap -> capAmounts(req, cap))
                .orElseGet(() -> ResultAmounts.uncapped(req.betAmount(), req.winAmount(), req.jackpotAmount()));
    }

    private Optional<BigDecimal> resolveCapAmount(CapRequest req, BigDecimal toVendorRate) {
        BigDecimal winAmount = req.winAmount();
        BigDecimal jackpotAmount = req.jackpotAmount();

        if ((winAmount == null || winAmount.signum() == 0) && (jackpotAmount == null || jackpotAmount.signum() == 0)) {
            return Optional.empty();
        }

        Optional<Agent> agent = getAgent(req.agentId());
        if (agent.isEmpty()) return Optional.empty();

        BigDecimal configCapAmount = payoutSettingsDataService.getMaxPayoutAmount(
                agent.get().getMasterAgentId(),
                agent.get().getId(),
                req.vendorId(),
                req.gameCategoryId(),
                req.currencyId()
        );

        if (configCapAmount == null || configCapAmount.signum() <= 0) return Optional.empty();

        // Config is in operator currency; convert into vendor units to compare against vendor-unit amounts.
        return Optional.of(TxnAmount.of(configCapAmount, toVendorRate).amount());
    }

    private boolean exceedsCap(CapRequest req, BigDecimal cap) {
        if (cap == null) return false;
        return normalize(req.winAmount()).compareTo(cap) > 0
                || normalize(req.jackpotAmount()).compareTo(cap) > 0;
    }

    private ResultAmounts capAmounts(CapRequest req, BigDecimal cap) {
        final BigDecimal bet = normalize(req.betAmount());
        final BigDecimal jackpot = normalize(req.jackpotAmount());
        final BigDecimal win = normalize(req.winAmount());

        final BigDecimal cappedJackpot = jackpot.min(cap).max(BigDecimal.ZERO);
        final BigDecimal cappedWin = win.min(cap).max(BigDecimal.ZERO);
        final BigDecimal cappedWinLoss = cappedWin.subtract(bet);

        return new ResultAmounts(cappedWin, cappedJackpot, cappedWinLoss, true);
    }

    // ---------------------------------------------------------------------
    // Legacy adapter: unchanged signature, delegates to the core. Only mutates
    // the BetInformation (and stashes uncap* fields) when a cap actually applies,
    // preserving the original behaviour.
    // ---------------------------------------------------------------------

    public BetInformation applyPayoutCap(BetInformation betInfo, BigDecimal toVendorRate) {
        ResultAmounts result = applyPayoutCap(
                new CapRequest(
                        betInfo.getAgentId(),
                        betInfo.getVendorId(),
                        betInfo.getGameCategoryId(),
                        betInfo.getCurrencyId(),
                        betInfo.getBetAmount(),
                        betInfo.getWinAmount(),
                        betInfo.getJackpotAmount()
                ),
                toVendorRate
        );

        if (result.capped()) {
            applyUpdatedAmount(betInfo, result.cappedWin(), result.cappedWinLoss(), result.cappedJackpot());
        }
        return betInfo;
    }

    public BetInformation applyUpdatedAmount(BetInformation betInfo,
                                             BigDecimal cappedWin,
                                             BigDecimal cappedWinLoss,
                                             BigDecimal cappedJackpot) {

        betInfo.setUncapWinAmount(betInfo.getWinAmount());
        betInfo.setUncapJackpotAmount(betInfo.getJackpotAmount());
        betInfo.setUncapWinLoss(betInfo.getWinLoss());
        betInfo.setUncapEffectiveTurnover(betInfo.getEffectiveTurnover());

        betInfo.setWinAmount(cappedWin);
        betInfo.setWinLoss(cappedWinLoss);
        betInfo.setJackpotAmount(cappedJackpot);

        return betInfo;
    }

    private Optional<Agent> getAgent(Integer agentId) {
        try {
            return Optional.of(agentService.get(agentId));
        } catch (EntityNotFoundException ex) {
            return Optional.empty();
        }
    }
}
