package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.vendor.VendorComponent;
import com.nextgen.gameaggregator.entity.ga.GameSession;

public interface BetLifeCycle extends VendorComponent {
    void onBeforeSend(GameSession gameSession, BetContext context);
}
