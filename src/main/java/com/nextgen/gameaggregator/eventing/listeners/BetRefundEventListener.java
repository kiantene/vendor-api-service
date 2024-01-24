package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.BetRefundLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.BetRollbackEvent;
import com.nextgen.gameaggregator.repository.ga.writer.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.ga.writer.BetRefundLogRepository;
import com.nextgen.gameaggregator.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BetRefundEventListener implements EventListener<BetRollbackEvent> {

    @Autowired
    private BetHistoryRepository betHistoryRepository;

    @Autowired
    private BetRefundLogRepository betRefundLogRepository;

    @Autowired
    private CachingService cachingService;

    @Override
    public void onEvent(BetRollbackEvent event) {
        BetHistory betHistory = event.getBetHistory();
        BetRefundLog betRefundLog = event.getBetRefundLog();


        if(betRefundLog.getOperatorStatus() !=1){
            betRefundLog.setOperatorStatus(1);
            betRefundLogRepository.save(betRefundLog);
        }

        // TODO: To add refund logic for different statuses
        if ( BetStatus.UNSETTLED.isValueOf(betHistory.getStatus()) ) {
            betHistory.setStatus(BetStatus.REFUNDED.code);

            betHistoryRepository.save(betHistory);
            cachingService.updateBetHistoriesCaching(betHistory);
        }
    }
}
