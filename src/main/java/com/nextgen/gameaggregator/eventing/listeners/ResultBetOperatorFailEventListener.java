package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.UnsettledBetResult;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.ResultBetOperatorFailEvent;
import com.nextgen.gameaggregator.repository.RawResultBetRepository;
import com.nextgen.gameaggregator.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ResultBetOperatorFailEventListener implements EventListener<ResultBetOperatorFailEvent> {
    @Autowired
    private RawResultBetRepository rawResultBetRepository;

    @Autowired
    private CachingService cachingService;

    @Override
    public void onEvent(ResultBetOperatorFailEvent event) {
        UnsettledBetResult unsettledBetResult = event.getUnsettledBetResult();
        unsettledBetResult.setOperatorStatus(event.getResponseCode());
        rawResultBetRepository.save(unsettledBetResult);
        cachingService.updateResultBetCaching(unsettledBetResult);
    }
}
