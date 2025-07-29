package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.entity.Currency;
import com.nextgen.gameaggregator.core.exception.GameCategoryNotFoundException;
import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.exception.VendorGameNotFoundException;
import com.nextgen.gameaggregator.core.exception.VendorNotFoundException;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.CurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.service.data.GameCategoryDataService;
import com.nextgen.gameaggregator.core.service.data.VendorDataService;
import com.nextgen.gameaggregator.core.service.data.VendorGameDataService;
import com.nextgen.gameaggregator.entity.ga.GameCategory;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import org.springframework.stereotype.Service;

@Service
public class PromoPayoutContextEnricher extends BaseEnricher<PromoPayoutContext> {
    private final CurrencyDataService currencyDataService;
    private final VendorDataService vendorDataService;
    private final VendorGameDataService vendorGameDataService;
    private final GameCategoryDataService gameCategoryDataService;

    public PromoPayoutContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                      VendorPlayerDataService vendorPlayerDataService,
                                      CurrencyDataService currencyDataService,
                                      VendorDataService vendorDataService,
                                      VendorGameDataService vendorGameDataService,
                                      GameCategoryDataService gameCategoryDataService) {
        super(agentPlayerDataService, vendorPlayerDataService);
        this.currencyDataService = currencyDataService;
        this.vendorDataService = vendorDataService;
        this.vendorGameDataService = vendorGameDataService;
        this.gameCategoryDataService = gameCategoryDataService;
    }

    public void doEnrich(PromoPayoutContext context) {
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
            // TODO : add constructor to support root cause
        }
    }

    private void populateVendor(PromoPayoutContext context) {
        try {
            Vendor vendor = vendorDataService.get(context.getVendorId());
            context.setVendorCode(vendor.getCode());
        } catch (VendorNotFoundException e) {
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
        } catch (VendorGameNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }

    private void populateGameCategory(PromoPayoutContext context) {
        try {
            GameCategory gameCategory = gameCategoryDataService.get(context.getGameCategoryId());
            context.setGameCategoryCode(gameCategory.getCode());
        } catch (GameCategoryNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }
}
