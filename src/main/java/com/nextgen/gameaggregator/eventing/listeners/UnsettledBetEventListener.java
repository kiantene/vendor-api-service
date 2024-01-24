package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.UnsettledBetEvent;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.repository.ga.writer.RawUnsettledBetRepository;
import com.nextgen.gameaggregator.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UnsettledBetEventListener implements EventListener<UnsettledBetEvent> {

    @Autowired
    private RawUnsettledBetRepository rawUnsettledBetRepository;

    @Autowired
    private CachingService cachingService;

    @Override
    public void onEvent(UnsettledBetEvent event) {

        UnsettledBet unsettledBet = event.getUnsettledBet();
        Integer statusOk = ResponseCodes.Status.SC_OK.code;

        // update operator status if previous was failed
        if (!unsettledBet.getOperatorStatus().equals(statusOk)) {
            unsettledBet.setOperatorStatus(statusOk);
            rawUnsettledBetRepository.save(unsettledBet);
            cachingService.updateUnsettledBetCaching(unsettledBet);
        }
    }
}
