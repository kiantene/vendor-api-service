package com.nextgen.gameaggregator.event;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.enums.BetStatus;
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
        String betId = resultLog.getReferenceTransactionId();
        // TODO: performance tuning
        Optional<BetHistory> betHistory = betHistoryRepository.findById(betId);

        if (betHistory.isPresent()) {
            // TODO: to review the following business logic, in case of data overwritten
            BetHistory history = betHistory.get();
            if (history.getStatus().equals(BetStatus.UNSETTLED.code)) {
                history.setWinAmount(resultLog.getWinAmount());
                history.setWinLoss(resultLog.getWinAmount());
                history.setEffectiveTurnover(resultLog.getWinAmount());
                history.setResultType(resultLog.getResultType());
                history.setVendorSettleTime(resultLog.getVendorTime());

                betHistoryRepository.save(history);
            }
        } else {
            log.error("cannot find bet: " + betId);
        }

//        BetHistory betHistory = new BetHistory();
//        betHistory.setId(resultLog.getReferenceTransactionId());
//        betHistory.setWinAmount(resultLog.getWinAmount());
//        betHistory.setWinLoss(resultLog.getWinAmount());
//        betHistory.setEffectiveTurnover(resultLog.getWinAmount());
//        betHistory.setResultType(resultLog.getResultType());
//        betHistory.setVendorSettleTime(resultLog.getVendorTime());
//
//        betHistoryRepository.save(betHistory);
    }
}
