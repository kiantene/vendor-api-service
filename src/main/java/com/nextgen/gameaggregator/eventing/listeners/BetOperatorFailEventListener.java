package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.BetOperatorFailEvent;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BetOperatorFailEventListener implements EventListener<BetOperatorFailEvent> {
    @Autowired
    private BetHistoryRepository betHistoryRepository;

    @Override
    public void onEvent(BetOperatorFailEvent event) {
        BetHistory betHistory = event.getBetHistory();

        if( InsufficientBalanceException.class.getSimpleName().equals(event.getExceptionName())){
            betHistory.setOperatorStatus(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
        }else if(InvalidOperatorResponseException.class.getSimpleName().equals(event.getExceptionName())){
            betHistory.setOperatorStatus(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
        }

        betHistoryRepository.save(betHistory);
    }
}
