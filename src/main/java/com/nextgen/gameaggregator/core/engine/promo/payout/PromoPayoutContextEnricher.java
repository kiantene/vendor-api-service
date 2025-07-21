package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.service.data.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.data.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.service.*;
import org.springframework.stereotype.Service;

@Service
public class PromoPayoutContextEnricher extends BaseEnricher<PromoPayoutContext> {
//    private final AgentService agentService;
//    private final CurrencyService currencyService;
//    private final VendorGameService vendorGameService;
//    private final VendorLineService vendorLineService;
//    private final VendorService vendorService;
//    private final GameCategoryService gameCategoryService;

    public PromoPayoutContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                      VendorPlayerDataService vendorPlayerDataService) {
        super(agentPlayerDataService, vendorPlayerDataService);
    }

    public void doEnrich(PromoPayoutContext context) {
        try {

//            Vendor vendor = vendorService.getById(context.getVendorId());
//            context.setVendorId(vendor.getId());
//            context.setVendorCode(vendor.getCode());

//            VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(context.getVendorGameCode(), context.getVendorId());
//            context.setVendorGameId(vendorGame.getId());
//            context.setGameCode(vendorGame.getCode());

//            GameCategory gameCategory = gameCategoryService.getByGameCategoryId(vendorGame.getGameCategoryId());
//            context.setGameCategoryId(gameCategory.getId());
//            context.setGameCategoryCode(gameCategory.getCode());

//            Currency currency = currencyService.get(vendorPlayer.getCurrencyId());
//            context.setCurrencyId(currency.getId());
//            context.setCurrency(currency.getCode());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
