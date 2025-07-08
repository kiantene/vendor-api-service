package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.common.ContextEnricher;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.service.AgentPlayerService;
import com.nextgen.gameaggregator.service.AgentService;
import com.nextgen.gameaggregator.service.CurrencyService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromoPayoutContextEnricher implements ContextEnricher<PromoPayoutContext> {
    private final AgentService agentService;
    private final VendorPlayerService vendorPlayerService;
    private final AgentPlayerService agentPlayerService;
    private final CurrencyService currencyService;

    public void enrich(PromoPayoutContext context) {
        try {
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(context.getVendorPlayerUsername());
            context.setVendorPlayerId(vendorPlayer.getId());

            AgentPlayer agentPlayer = agentPlayerService.get(vendorPlayer.getAgentPlayerId());
            context.setAgentId(agentPlayer.getAgentId());
            context.setAgentPlayerId(agentPlayer.getId());
            context.setAgentPlayerUsername(agentPlayer.getUsername());
            Currency currency = currencyService.get(vendorPlayer.getCurrencyId());
            context.setCurrency(currency.getCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
