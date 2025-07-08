package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.common.ContextEnricher;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromoPayoutContextEnricher implements ContextEnricher<PromoPayoutContext> {
    private final AgentService agentService;
    private final VendorPlayerService vendorPlayerService;
    private final AgentPlayerService agentPlayerService;
    private final CurrencyService currencyService;
    private final VendorGameService vendorGameService;
    private final VendorLineService vendorLineService;
    private final VendorService vendorService;
    private final GameCategoryService gameCategoryService;

    public void enrich(PromoPayoutContext context) {
        try {

            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(context.getVendorPlayerUsername());
            context.setVendorPlayerId(vendorPlayer.getId());
            context.setVendorLineId(vendorPlayer.getVendorLineId());

            AgentPlayer agentPlayer = agentPlayerService.get(vendorPlayer.getAgentPlayerId());
            context.setAgentId(agentPlayer.getAgentId());
            context.setAgentPlayerId(agentPlayer.getId());
            context.setAgentPlayerUsername(agentPlayer.getUsername());

            Vendor vendor = vendorService.getById(vendorPlayer.getVendorId());
            context.setVendorId(vendor.getId());
            context.setVendorCode(vendor.getCode());

            VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(context.getVendorGameCode(), context.getVendorId());
            context.setVendorGameId(vendorGame.getId());
            context.setGameCode(vendorGame.getCode());

            GameCategory gameCategory = gameCategoryService.getByGameCategoryId(vendorGame.getGameCategoryId());
            context.setGameCategoryId(gameCategory.getId());
            context.setGameCategoryCode(gameCategory.getCode());

            Currency currency = currencyService.get(vendorPlayer.getCurrencyId());
            context.setCurrencyId(currency.getId());
            context.setCurrency(currency.getCode());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
