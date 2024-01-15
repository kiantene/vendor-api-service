package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.ga.BetResultLog;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.BetResultOperatorFailEvent;
import com.nextgen.gameaggregator.repository.ga.writer.BetResultLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BetResultOperatorFailEventListener implements EventListener<BetResultOperatorFailEvent> {

    @Autowired
    private BetResultLogRepository betResultLogRepository;

    @Override
    public void onEvent(BetResultOperatorFailEvent event) {
        BetResultLog betResultLog = event.getBetResultLog();
        betResultLog.setOperatorStatus(event.getResponseCode());
        betResultLogRepository.save(betResultLog);
    }
}
