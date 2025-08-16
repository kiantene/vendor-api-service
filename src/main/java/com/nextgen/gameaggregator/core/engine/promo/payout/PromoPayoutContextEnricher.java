package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.entity.*;
import com.nextgen.gameaggregator.core.service.*;
import org.springframework.stereotype.Service;

@Service
public class PromoPayoutContextEnricher extends BaseEnricher<PromoPayoutContext> {
    private final CurrencyDataService currencyDataService;
    private final VendorDataService vendorDataService;
    private final VendorGameDataService vendorGameDataService;
    private final GameCategoryDataService gameCategoryDataService;
    private final AgentDataService agentDataService;

    public PromoPayoutContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                      VendorPlayerDataService vendorPlayerDataService,
                                      CurrencyDataService currencyDataService,
                                      VendorDataService vendorDataService,
                                      VendorGameDataService vendorGameDataService,
                                      GameCategoryDataService gameCategoryDataService,
                                      AgentDataService agentDataService) {
        super(agentPlayerDataService, vendorPlayerDataService);
        this.currencyDataService = currencyDataService;
        this.vendorDataService = vendorDataService;
        this.vendorGameDataService = vendorGameDataService;
        this.gameCategoryDataService = gameCategoryDataService;
        this.agentDataService = agentDataService;
    }

    public void doEnrich(PromoPayoutContext context) {
        this.populateAgent(context);
        this.populateCurrency(context);
        this.populateVendor(context);
        this.populateVendorGame(context);
        this.populateGameCategory(context);
    }

    private void populateCurrency(PromoPayoutContext context) {
        try {
            Currency currency = currencyDataService.get(context.getCurrencyId());
            context.setCurrencyId(currency.getId());
            context.setCurrency(currency.getCode());
        } catch (EntityNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }

    private void populateAgent(PromoPayoutContext context) {
        try {
            Agent agent = agentDataService.get(context.getAgentId());
            context.setMasterAgentId(agent.getMasterAgentId());
            context.setHouseId(agent.getHouseId());
        } catch (EntityNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }

    private void populateVendor(PromoPayoutContext context) {
        try {
            Vendor vendor = vendorDataService.get(context.getVendorId());
            context.setVendorCode(vendor.getCode());
        } catch (EntityNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }

    private void populateVendorGame(PromoPayoutContext context) {
        try {
            VendorGame vendorGame = vendorGameDataService.getByVendorGameCodeAndVendorId(context.getVendorGameCode(), context.getVendorId());
            context.setVendorGameId(vendorGame.getId());
            context.setGameCode(vendorGame.getCode());
            context.setGameName(vendorGame.getName());
            context.setGameCategoryId(vendorGame.getGameCategoryId());
        } catch (EntityNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }

    private void populateGameCategory(PromoPayoutContext context) {
        try {
            GameCategory gameCategory = gameCategoryDataService.get(context.getGameCategoryId());
            context.setGameCategoryCode(gameCategory.getCode());
        } catch (EntityNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }
}
