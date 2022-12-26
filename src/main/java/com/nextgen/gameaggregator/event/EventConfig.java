package com.nextgen.gameaggregator.event;

import com.nextgen.gameaggregator.vendor.pragmaticplay.api.bet.BetAction;
import com.nextgen.gameaggregator.vendor.pragmaticplay.api.bet.BetEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EventConfig {
    @Autowired
    private BetEventHandler betEventHandler;

    @Bean("eventListeners")
    public void eventListeners() {
        EventDispatcher.addListener(BetAction.class, betEventHandler);
    }
}
