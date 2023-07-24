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
    @Autowired
    private BetResultOperatorFailEventListener betResultOperatorFailEventListener;
    @Autowired
    private BetRefundOperatorFailEventListener betRefundOperatorFailEventListener;
    @Autowired
    private UnsettledBetEventListener unsettledBetEventListener;
    @Autowired
    private UnsettledBetOperatorFailEventListener unsettledBetOperatorFailEventListener;
    @Autowired
    private SettledBetEventListener settledBetEventListener;
    @Autowired
    private SettledBetOperatorFailEventListener settledBetOperatorFailEventListener;

    @Bean("eventListeners")
    public void eventListeners() {
        EventDispatcherSystem.addListener(BetEvent.class, betEventListener);
        EventDispatcherSystem.addListener(BetResultEvent.class, betResultEventListener);
        EventDispatcherSystem.addListener(EndRoundEvent.class, endRoundEventListener);
        EventDispatcherSystem.addListener(BetRollbackEvent.class, betRefundEventListener);
        EventDispatcherSystem.addListener(BetOperatorFailEvent.class, betOperatorFailEventListener);
        EventDispatcherSystem.addListener(BetResultOperatorFailEvent.class, betResultOperatorFailEventListener);
        EventDispatcherSystem.addListener(BetRefundOperatorFailEvent.class, betRefundOperatorFailEventListener);

        EventDispatcherSystem.addListener(UnsettledBetEvent.class, unsettledBetEventListener);
        EventDispatcherSystem.addListener(UnsettledBetOperatorFailEvent.class, unsettledBetOperatorFailEventListener);
        EventDispatcherSystem.addListener(SettledBetEvent.class, settledBetEventListener);
        EventDispatcherSystem.addListener(SettledBetOperatorFailEvent.class, settledBetOperatorFailEventListener);
    }
}
