package com.nextgen.gameaggregator.vendor.ezugi.api.v2.handler;

import org.springframework.stereotype.Component;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultLifeCycle;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;

@Component
public class BeforeSendBetResultHandler implements BetResultLifeCycle {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public void onBeforeSend(GameSession gameSession, BetResultContext context) {
        if (!context.getVendorGameId().equals(gameSession.getVendorGameId())) {
            gameSession.setVendorGameId(context.getVendorGameId());
        }
    }
}
