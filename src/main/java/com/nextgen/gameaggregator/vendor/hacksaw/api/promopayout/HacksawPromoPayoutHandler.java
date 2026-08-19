package com.nextgen.gameaggregator.vendor.hacksaw.api.promopayout;

import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutConfig;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.hacksaw.vo.ResponseVo;
import org.springframework.stereotype.Component;

@Component
public class HacksawPromoPayoutHandler extends AbstractPromoPayoutController<PromoPayoutDto, ResponseVo> {

    public HacksawPromoPayoutHandler(HacksawPromoPayoutRequestMapper requestMapper,
                                     HacksawPromoPayoutResponseMapper responseMapper,
                                     PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    public ResponseVo process(PromoPayoutDto request) {
        return processRequest(request);
    }

    @Override
    protected void configure(PromoPayoutConfig config, PromoPayoutDto request) {
        config.playerUuidCampaignLookup(true);
    }
}
