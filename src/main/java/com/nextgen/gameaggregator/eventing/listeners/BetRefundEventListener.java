package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.BetRefundEvent;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class BetRefundEventListener implements EventListener<BetRefundEvent> {

    @Autowired
    private BetHistoryRepository betHistoryRepository;

    @Override
    public void onEvent(BetRefundEvent event) {
        BetHistory betHistory = event.getBetHistory();

        // TODO: To add refund logic for different statuses
        if ( BetStatus.UNSETTLED.isValueOf(betHistory.getStatus()) ) {
            BigDecimal refundAmount = betHistory.getBetAmount();
            betHistory.setRefundAmount(refundAmount);
            betHistory.setRefundTime(System.currentTimeMillis());
            betHistory.setStatus(BetStatus.REFUNDED.code);

            betHistoryRepository.save(betHistory);
        }
    }
}
