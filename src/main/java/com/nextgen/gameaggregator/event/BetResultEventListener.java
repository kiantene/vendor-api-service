package com.nextgen.gameaggregator.event;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class BetResultEventListener implements EventListener<BetResultEvent> {

    @Autowired
    private BetHistoryRepository betHistoryRepository;

    @Override
    public void onEvent(BetResultEvent event) {
        BetResultLog resultLog = event.getData();
        // TODO: performance tuning
        Optional<BetHistory> betHistory = betHistoryRepository.findById(resultLog.getReferenceTransactionId());

        if (betHistory.isPresent()) {
            BetHistory history = betHistory.get();
            history.setWinAmount(resultLog.getWinAmount());
            history.setWinLoss(resultLog.getWinAmount());
            history.setEffectiveTurnover(resultLog.getWinAmount());
            history.setResultType(resultLog.getResultType());
            history.setStatus(2); // TODO: refactor
            history.setVendorSettleTime(resultLog.getVendorTime());

            betHistoryRepository.save(history);
        }
    }
}
