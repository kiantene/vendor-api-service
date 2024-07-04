package com.nextgen.gameaggregator.scheduler;

import com.nextgen.gameaggregator.entity.ga.RawBetResultRetryLog;
import com.nextgen.gameaggregator.repository.ga.writer.RawBetResultRetryLogRepository;
import com.nextgen.gameaggregator.service.BetResultRetryLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BetResultRetryScheduler {

    private final RawBetResultRetryLogRepository rawBetResultRetryLogRepository;
    private final BetResultRetryLogService betResultRetryLogService;
    @Value("${scheduler.bet-result-retry:false}")
    private Boolean useScheduler;

    public BetResultRetryScheduler(RawBetResultRetryLogRepository rawBetResultRetryLogRepository,
                                   BetResultRetryLogService betResultRetryLogService) {
        this.rawBetResultRetryLogRepository = rawBetResultRetryLogRepository;
        this.betResultRetryLogService = betResultRetryLogService;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void processBetResultRetryLog() {

        Long currentTime = System.currentTimeMillis() + 1;
        Integer maxRetryCounter = betResultRetryLogService.maxRetryCounter;
        Pageable pageable = PageRequest.of(0, 20);
        Thread currentThread = Thread.currentThread();

        if (!useScheduler) {
            //Do nothing

        } else {
            //get maximum 10 records once per query
            Page<RawBetResultRetryLog> rawBetResultRetryLogPage = rawBetResultRetryLogRepository.findByNextRetryTimeLessThanAndRetryCounterLessThan(currentTime, maxRetryCounter, pageable);
            List<RawBetResultRetryLog> rawBetResultRetryLogList = rawBetResultRetryLogPage.getContent();

            //sort by createDate ascending
            rawBetResultRetryLogList = rawBetResultRetryLogList.stream()
                    .sorted(Comparator.comparingLong(RawBetResultRetryLog::getCreateDate))
                    .collect(Collectors.toList());

            betResultRetryLogService.asyncProcessRetryRequestByList(rawBetResultRetryLogList, currentTime);
        }
    }
}
