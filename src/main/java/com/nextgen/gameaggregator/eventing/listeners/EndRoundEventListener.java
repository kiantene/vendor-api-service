package com.nextgen.gameaggregator.eventing.listeners;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.core.EventListener;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.service.CachingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class EndRoundEventListener implements EventListener<EndRoundEvent> {

    @Autowired
    private BetHistoryRepository betHistoryRepository;

    @Autowired
    private CachingService cachingService;

    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;

    @Override
    public void onEvent(EndRoundEvent event) {
        BetHistory betHistory = event.getBetHistory();
        Long currentTimestamp = System.currentTimeMillis();

        // Process only if status is Unsettled
        if (BetStatus.UNSETTLED.isValueOf(betHistory.getStatus())) {
            betHistory.setStatus(BetStatus.SETTLED.code);

            // TODO: to review this logic
            /** This piece of code causing PGSoft bet not working as intended cause if resultType is Lose, winLoss will not be calculated
            if (betHistory.getResultType().equals(WinType.LOSE.code)) {
                BigDecimal betAmount = betHistory.getBetAmount();
                betHistory.setWinLoss(betAmount.negate());
            }
             **/
            BigDecimal betAmount = betHistory.getBetAmount();
            BigDecimal winAmount = betHistory.getWinAmount();
            betHistory.setWinLoss(winAmount.subtract(betAmount));

            if (betHistory.getVendorSettleTime() == null) {
                betHistory.setVendorSettleTime(currentTimestamp);
            }
            if (betHistory.getResultTime() == null) {
                betHistory.setResultTime(currentTimestamp);
            }

            betHistoryRepository.save(betHistory);
            cachingService.updateBetHistoriesCaching(betHistory);

            Gson gson = new GsonBuilder().create();
            // TODO - to move topic name into constant
            stringKafkaTemplate.send("topic_data_aggregate_new", betHistory.getId(), gson.toJson(betHistory));
        }

    }
}
