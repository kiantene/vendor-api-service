package com.nextgen.gameaggregator.scheduler.betaction;

import com.nextgen.gameaggregator.entity.ga.RawBetActionLog;
import com.nextgen.gameaggregator.repository.ga.writer.RawBetActionLogRepository;
import com.nextgen.gameaggregator.service.BetActionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
public class BetActionScheduler {
    private final BetActionLogService betActionLogService;
    private final RawBetActionLogRepository rawBetActionLogRepository;
    @Value("${scheduler.bet-action:false}")
    private boolean useScheduler;
    public BetActionScheduler(BetActionLogService betActionLogService, RawBetActionLogRepository rawBetActionLogRepository) {
        this.betActionLogService = betActionLogService;
        this.rawBetActionLogRepository = rawBetActionLogRepository;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void processBetActionLog() {
        long currentTime = System.currentTimeMillis() + 1;
        Integer maxRetryCounter = BetActionLogService.MAX_RETRY_COUNTER;

        if (useScheduler) {
            //get maximum 10 records once per query
            List<RawBetActionLog> rawBetActionLogList = rawBetActionLogRepository.findTop100ByNextRetryTimeLessThanAndRetryCounterLessThan(currentTime, maxRetryCounter);

            if (!rawBetActionLogList.isEmpty()) {
                //sort by createDate ascending
                rawBetActionLogList = rawBetActionLogList.stream()
                        .sorted(Comparator.comparingLong(RawBetActionLog::getCreateDate))
                        .toList();

                betActionLogService.asyncProcessRetryRequestByList(rawBetActionLogList, currentTime);
            }
        }

    }
}
