package com.nextgen.gameaggregator.event;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.enums.BetStatus;
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

        if (betHistory.getStatus().equals(BetStatus.UNSETTLED.code)) {
            betHistory.setStatus(BetStatus.SETTLED.code);
            if (betHistory.getVendorSettleTime() == null) {
                betHistory.setVendorSettleTime(System.currentTimeMillis());
            }

            betHistoryRepository.save(betHistory);
        }
    }
}
