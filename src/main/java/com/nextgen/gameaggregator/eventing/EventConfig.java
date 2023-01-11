package com.nextgen.gameaggregator.eventing;

import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.*;
import com.nextgen.gameaggregator.eventing.listeners.*;
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
    @Autowired
    private BetRefundEventListener betRefundEventListener;
    @Autowired
    private BetEventListener betEventListener;
    @Autowired
    private BetOperatorFailEventListener betOperatorFailEventListener;

    @Bean("eventListeners")
    public void eventListeners() {
        EventDispatcherSystem.addListener(BetEvent.class, betEventListener);
        EventDispatcherSystem.addListener(BetResultEvent.class, betResultEventListener);
        EventDispatcherSystem.addListener(EndRoundEvent.class, endRoundEventListener);
        EventDispatcherSystem.addListener(BetRefundEvent.class, betRefundEventListener);
        EventDispatcherSystem.addListener(BetOperatorFailEvent.class, betOperatorFailEventListener);
    }
}
