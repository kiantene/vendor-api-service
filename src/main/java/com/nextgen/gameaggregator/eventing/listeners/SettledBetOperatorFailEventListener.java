package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.RawResultBet;
import com.nextgen.gameaggregator.entity.RawSettledBet;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.ResultBetOperatorFailEvent;
import com.nextgen.gameaggregator.eventing.events.SettledBetOperatorFailEvent;
import com.nextgen.gameaggregator.repository.RawResultBetRepository;
import com.nextgen.gameaggregator.repository.RawSettledBetRepository;
import com.nextgen.gameaggregator.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SettledBetOperatorFailEventListener implements EventListener<SettledBetOperatorFailEvent> {
    @Autowired
    private RawSettledBetRepository rawSettledBetRepository;

    @Autowired
    private CachingService cachingService;

    @Override
    public void onEvent(SettledBetOperatorFailEvent event) {
        RawSettledBet rawSettledBet = event.getRawSettledBet();
        rawSettledBet.setOperatorStatus(event.getResponseCode());
        rawSettledBetRepository.save(rawSettledBet);
        cachingService.updateSettledBetCaching(rawSettledBet);
    }
}
