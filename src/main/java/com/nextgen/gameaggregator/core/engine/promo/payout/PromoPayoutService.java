package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

import java.util.function.Consumer;

public interface PromoPayoutService {
    PlayerBalanceData process(PromoPayoutContext context);
    PromoPayoutService initialise(PromoPayoutContext context);
    PromoPayoutService configure(Consumer<PromoPayoutConfig> configurer);
}
