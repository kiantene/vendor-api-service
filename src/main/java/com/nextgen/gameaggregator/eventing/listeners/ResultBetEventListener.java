package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.RawResultBet;
import com.nextgen.gameaggregator.entity.RawUnsettledBet;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.ResultBetEvent;
import com.nextgen.gameaggregator.eventing.events.UnsettledBetEvent;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.repository.RawResultBetRepository;
import com.nextgen.gameaggregator.repository.RawUnsettledBetRepository;
import com.nextgen.gameaggregator.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ResultBetEventListener implements EventListener<ResultBetEvent> {

    @Autowired
    private RawResultBetRepository rawResultBetRepository;

    @Autowired
    private CachingService cachingService;

    @Override
    public void onEvent(ResultBetEvent event) {

        RawResultBet rawResultBet = event.getRawResultBet();
        Integer statusOk = ResponseCodes.Status.SC_OK.code;

        // update operator status if previous was failed
        if (!rawResultBet.getOperatorStatus().equals(statusOk)) {
            rawResultBet.setOperatorStatus(statusOk);
            rawResultBetRepository.save(rawResultBet);
            cachingService.updateResultBetCaching(rawResultBet);
        }
    }
}
