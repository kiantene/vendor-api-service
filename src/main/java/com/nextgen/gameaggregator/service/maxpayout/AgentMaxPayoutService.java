package com.nextgen.gameaggregator.service.maxpayout;

import com.nextgen.gameaggregator.entity.ga.BetInformation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AgentMaxPayoutService {

    public AgentPayout applyPayoutCap(Integer agentId, Integer vendorId, Integer currencyId, BetInformation betInformation) {
        if (isZero(betInformation.getWinAmount()) || !isPayoutCapConfigured(agentId, vendorId, currencyId))
            return new AgentPayout();

        BigDecimal payoutCap = BigDecimal.TEN;
        return new AgentPayout(payoutCap, betInformation);
    }

    public boolean isPayoutCapConfigured(Integer agentId, Integer vendorId, Integer currencyId) {
        return true;
    }

    private boolean isZero(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
}
