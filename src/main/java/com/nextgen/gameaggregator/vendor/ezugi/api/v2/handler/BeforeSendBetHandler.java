package com.nextgen.gameaggregator.vendor.ezugi.api.v2.handler;

import org.springframework.stereotype.Component;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetLifeCycle;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BeforeSendBetHandler implements BetLifeCycle {

    @Override
    public String getVendorClassName() {
       return EndPoints.CLASS_NAME;
    }

    @Override
    public void onBeforeSend(GameSession gameSession, BetContext context) {
        if (!context.getVendorGameId().equals(gameSession.getVendorGameId())) {
            log.info("BeforeSendBetHandler : Updating vendor game ID from {} to {}", gameSession.getVendorGameId(), context.getVendorGameId());
            gameSession.setVendorGameId(context.getVendorGameId());
            gameSession.setVendorGameCode(context.getVendorGameCode());
            gameSession.setGameCode(context.getGameCode());
        }
    }
    
}
