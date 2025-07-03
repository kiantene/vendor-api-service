package com.nextgen.gameaggregator.core.engine.promo;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public interface PromoPayoutService {
    PlayerBalanceData process(PromoPayoutContext context);
}
