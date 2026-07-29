package com.nextgen.gameaggregator.vendor.habanero.api.bonus;

import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import org.springframework.stereotype.Component;

@Component
public class HabaneroBonusPayoutHandler
        extends AbstractPromoPayoutController<HabaneroBonusPayoutRequest, TransferVo> {

    public HabaneroBonusPayoutHandler(HabaneroBonusPayoutRequestMapper requestMapper,
                                      HabaneroBonusPayoutResponseMapper responseMapper,
                                      PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    public TransferVo process(HabaneroBonusPayoutRequest request) {
        return processRequest(request);
    }
}
