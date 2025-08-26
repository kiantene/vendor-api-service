package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.common.AbstractProcessorController;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public abstract class AbstractPromoPayoutController<Q, R> extends AbstractProcessorController<Q, R, PromoPayoutContext> {
    protected final PromoPayoutService promoPayoutService;

    protected AbstractPromoPayoutController(PromoPayoutContextMapper<Q> requestMapper,
                                            PromoPayoutVendorResponseMapper<R> responseMapper,
                                            PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper);
        this.promoPayoutService = promoPayoutService;
    }

    @Override
    protected PlayerBalanceData executeService(PromoPayoutContext context) {
        return promoPayoutService.process(context);
    }
}
