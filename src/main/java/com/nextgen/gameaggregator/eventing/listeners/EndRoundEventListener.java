package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EndRoundEventListener implements EventListener<EndRoundEvent> {

    @Autowired
    private BetHistoryRepository betHistoryRepository;

    @Override
    public void onEvent(EndRoundEvent event) {
        BetHistory betHistory = event.getBetHistory();
        Long currentTimestamp = System.currentTimeMillis();

        // Process only if status is Unsettled
        if (BetStatus.UNSETTLED.isValueOf(betHistory.getStatus())) {
            betHistory.setStatus(BetStatus.SETTLED.code);
            if (betHistory.getVendorSettleTime() == null) {
                betHistory.setVendorSettleTime(currentTimestamp);
            }
            if (betHistory.getResultTime() == null) {
                betHistory.setResultTime(currentTimestamp);
            }

            betHistoryRepository.save(betHistory);
        }
    }
}
