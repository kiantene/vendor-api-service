package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;

public interface PromoPayoutService {
    PlayerBalanceData process(PromoPayoutContext context);
}
