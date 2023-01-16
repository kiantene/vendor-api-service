package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.BetOperatorFailEvent;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BetOperatorFailEventListener implements EventListener<BetOperatorFailEvent> {
    @Autowired
    private BetHistoryRepository betHistoryRepository;

    @Autowired
    private CachingService cachingService;

    @Override
    public void onEvent(BetOperatorFailEvent event) {
        BetHistory betHistory = event.getBetHistory();
        betHistory.setOperatorStatus(event.getResponseCode());
        betHistoryRepository.save(betHistory);
        cachingService.updateBetHistoriesCaching(betHistory);
    }
}
