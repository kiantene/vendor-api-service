package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.service.data.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.data.VendorPlayerDataService;
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
//            this.populateVendorPlayer(context);
//            this.populateAgentPlayer(context);
//            this.populateVendor(context);
//            this.populateVendorGame(context);
//            this.populateGameCategory(context);
//            this.populateCurrency(context);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    private void populateVendorPlayer(PromoPayoutContext context) throws InvalidPlayerException {
//        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(context.getVendorPlayerUsername());
//        context.setVendorPlayerId(vendorPlayer.getId());
//        context.setVendorLineId(vendorPlayer.getVendorLineId());
//    }
//
//    private void populateAgentPlayer(PromoPayoutContext context) throws RecordNotFoundException {
//        AgentPlayer agentPlayer = agentPlayerService.get(context.getAgentPlayerId());
//        context.setAgentId(agentPlayer.getAgentId());
//        context.setAgentPlayerId(agentPlayer.getId());
//        context.setAgentPlayerUsername(agentPlayer.getUsername());
//    }
//
//    private void populateVendor(PromoPayoutContext context) throws InvalidVendorException {
//        Vendor vendor = vendorService.getById(context.getVendorId());
//        context.setVendorId(vendor.getId());
//        context.setVendorCode(vendor.getCode());
//    }
//
//    private void populateVendorGame(PromoPayoutContext context) throws GameNotSupportedException {
//        VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(context.getVendorGameCode(), context.getVendorId());
//        context.setVendorGameId(vendorGame.getId());
//        context.setGameCode(vendorGame.getCode());
//    }
//
//    private void populateGameCategory(PromoPayoutContext context) {
//        GameCategory gameCategory = gameCategoryService.getByGameCategoryId(context.getGameCategoryId());
//        context.setGameCategoryId(gameCategory.getId());
//        context.setGameCategoryCode(gameCategory.getCode());
//    }
//
//    private void populateCurrency(PromoPayoutContext context) throws InvalidCurrencyException {
//        Currency currency = currencyService.get(context.getCurrencyId());
//        context.setCurrencyId(currency.getId());
//        context.setCurrency(currency.getCode());
//    }


}
