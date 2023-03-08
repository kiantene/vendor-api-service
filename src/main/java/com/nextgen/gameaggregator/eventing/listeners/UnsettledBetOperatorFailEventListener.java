package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.RawUnsettledBet;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.BetOperatorFailEvent;
import com.nextgen.gameaggregator.eventing.events.UnsettledBetOperatorFailEvent;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.RawUnsettledBetRepository;
import com.nextgen.gameaggregator.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UnsettledBetOperatorFailEventListener implements EventListener<UnsettledBetOperatorFailEvent> {
    @Autowired
    private RawUnsettledBetRepository rawUnsettledBetRepository;

    @Autowired
    private CachingService cachingService;

    @Override
    public void onEvent(UnsettledBetOperatorFailEvent event) {
        RawUnsettledBet rawUnsettledBet = event.getRawUnsettledBet();
        rawUnsettledBet.setOperatorStatus(event.getResponseCode());
        rawUnsettledBetRepository.save(rawUnsettledBet);
        cachingService.updateUnsettledBetCaching(rawUnsettledBet);
    }
}
