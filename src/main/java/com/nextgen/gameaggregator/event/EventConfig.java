package com.nextgen.gameaggregator.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EventConfig {

    @Autowired
    private BetResultEventListener betResultEventListener;
    @Autowired
    private EndRoundEventListener endRoundEventListener;

    @Bean("eventListeners")
    public void eventListeners() {
        EventDispatcherSystem.addListener(BetResultEvent.class, betResultEventListener);
        EventDispatcherSystem.addListener(EndRoundEvent.class, endRoundEventListener);
    }
}
