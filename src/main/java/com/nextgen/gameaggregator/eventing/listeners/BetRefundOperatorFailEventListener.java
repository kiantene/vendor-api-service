package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.BetRefundLog;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.BetRefundOperatorFailEvent;
import com.nextgen.gameaggregator.repository.BetRefundLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BetRefundOperatorFailEventListener implements EventListener<BetRefundOperatorFailEvent> {

    @Autowired
    private BetRefundLogRepository betRefundLogRepository;

    @Override
    public void onEvent(BetRefundOperatorFailEvent event) {
        BetRefundLog betRefundLog = event.getBetRefundLog();
        betRefundLog.setOperatorStatus(event.getResponseCode());
        betRefundLogRepository.save(betRefundLog);
    }
}
