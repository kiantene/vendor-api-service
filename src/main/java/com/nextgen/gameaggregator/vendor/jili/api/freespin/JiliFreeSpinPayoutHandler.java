package com.nextgen.gameaggregator.vendor.jili.api.freespin;

import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutConfig;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.jili.api.bet.BetVo;
import org.springframework.stereotype.Component;

@Component
public class JiliFreeSpinPayoutHandler
        extends AbstractPromoPayoutController<JiliFreeSpinPayoutRequest, BetVo> {

    public JiliFreeSpinPayoutHandler(JiliFreeSpinPayoutRequestMapper requestMapper,
                                     JiliFreeSpinPayoutResponseMapper responseMapper,
                                     PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    public BetVo process(JiliFreeSpinPayoutRequest request) {
        return processRequest(request);
    }

    @Override
    protected void configure(PromoPayoutConfig config, JiliFreeSpinPayoutRequest request) {
        config.playerUuidCampaignLookup(true);
    }
}
