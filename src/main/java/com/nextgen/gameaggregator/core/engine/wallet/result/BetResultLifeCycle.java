package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.vendor.VendorComponent;
import com.nextgen.gameaggregator.entity.ga.GameSession;

public interface BetResultLifeCycle extends VendorComponent{
    void onBeforeSend(GameSession gameSession, BetResultContext context);
}
