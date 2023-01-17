package com.nextgen.gameaggregator.eventing.listeners;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class BetResultEventListener implements EventListener<BetResultEvent> {

    @Autowired
    private BetHistoryRepository betHistoryRepository;

    @Autowired
    private CachingService cachingService;

    @Override
    public void onEvent(BetResultEvent event) {
        BetHistory betHistory = event.getBetHistory();
        BetResultLog resultLog = event.getBetResultLog();

        // TODO: to review the following business logic, in case of data overwritten
        if (BetStatus.UNSETTLED.isValueOf(betHistory.getStatus())) {
            BigDecimal betAmount = betHistory.getBetAmount();
            BigDecimal winAmount = betHistory.getWinAmount();
            BigDecimal finalWinAmount = winAmount.add(resultLog.getWinAmount());
            BigDecimal winLoss = finalWinAmount.subtract(betAmount);

            betHistory.setWinAmount(finalWinAmount);
            betHistory.setWinLoss(winLoss);
            betHistory.setEffectiveTurnover(betAmount); // TODO: to confirm logic of effective turnover
            betHistory.setResultType(resultLog.getResultType());
            betHistory.setVendorSettleTime(resultLog.getVendorTime());
            betHistory.setResultTime(System.currentTimeMillis());
            // Status is updated during EndRound

            betHistoryRepository.save(betHistory);
            cachingService.updateBetHistoriesCaching(betHistory);
        }
    }
}
