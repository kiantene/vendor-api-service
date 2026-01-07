package com.nextgen.gameaggregator.vendor.gpkv2.api.handler;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultLifeCycle;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.EndPoints;
import org.springframework.stereotype.Component;

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
            gameSession.setVendorGameCode(context.getVendorGameCode());
            gameSession.setGameCode(context.getGameCode());
        }
    }
}
