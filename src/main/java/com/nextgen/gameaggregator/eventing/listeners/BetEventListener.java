package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.repository.ga.writer.BetHistoryRepository;
import com.nextgen.gameaggregator.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BetEventListener implements EventListener<BetEvent> {

    @Autowired
    private BetHistoryRepository betHistoryRepository;

    @Autowired
    private CachingService cachingService;

    @Override
    public void onEvent(BetEvent event) {
//        BetHistory betHistory = event.getBetHistory();
//        Integer statusOk = ResponseCodes.Status.SC_OK.code;
//
//        // update operator status if previous was failed
//        if (!betHistory.getOperatorStatus().equals(statusOk)) {
//            betHistory.setOperatorStatus(statusOk);
//            betHistoryRepository.save(betHistory);
//            cachingService.updateBetHistoriesCaching(betHistory);
//        }
    }
}
