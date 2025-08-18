package com.nextgen.gameaggregator.service.maxpayout;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AgentMaxPayoutService {

    public AgentPayout applyPayoutCap(Integer agentId, Integer vendorId, Integer currencyId, BigDecimal winAmount) {
        //TODO: CONDITION MAY NEED ADD FOR SKIP END CONDITION.
        if (isZero(winAmount) || !isPayoutCapConfigured(agentId, vendorId, currencyId))
            return new AgentPayout(winAmount, winAmount);

        // TODO: need to check currency conversion

        BigDecimal payoutCap = BigDecimal.TEN;
        return new AgentPayout(winAmount, payoutCap);
    }

    public boolean isPayoutCapConfigured(Integer agentId, Integer vendorId, Integer currencyId) {
        return true;
    }

    private boolean isZero(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
}
