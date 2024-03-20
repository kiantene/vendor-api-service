package com.nextgen.gameaggregator.scheduler;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.RawBetResultRetryLog;
import com.nextgen.gameaggregator.enums.RetryStatus;
import com.nextgen.gameaggregator.repository.ga.writer.RawBetResultRetryLogRepository;
import com.nextgen.gameaggregator.service.BetResultRetryLogService;
import com.nextgen.gameaggregator.service.HttpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class BetResultRetryScheduler {

    @Autowired
    private RawBetResultRetryLogRepository rawBetResultRetryLogRepository;
    @Autowired
    private BetResultRetryLogService betResultRetryLogService;
    @Autowired
    private HttpService httpService;

    @Scheduled(fixedDelay = 3000, initialDelay = 3000)
    public void processBetResultRetryLog() {
        HttpRequestLog httpRequestLog;
        Long currentTime = System.currentTimeMillis();
        Integer maxRetryCounter = 6;
        Integer retrieveLimit = 10;
        Integer status = RetryStatus.FAILED.code;

        //get maximum 10 records once per query
        List<RawBetResultRetryLog> rawBetResultRetryLogList = rawBetResultRetryLogRepository.findByRetryCounterAndNextRetryTimeAndStatusAndLimit(maxRetryCounter, currentTime, status, retrieveLimit);

        for (RawBetResultRetryLog rawBetResultRetryLogItem : rawBetResultRetryLogList) {
            try {
                betResultRetryLogService.call(rawBetResultRetryLogItem.getOperatorData(), rawBetResultRetryLogItem.getAction(), rawBetResultRetryLogItem.getAgentId());
                rawBetResultRetryLogItem.setStatus(RetryStatus.SUCCESS.code);

            } catch (Exception exception) {
                rawBetResultRetryLogItem.setStatus(RetryStatus.FAILED.code);
                rawBetResultRetryLogItem.setRetryCounter(rawBetResultRetryLogItem.getRetryCounter() + 1);
                rawBetResultRetryLogItem.setNextRetryTime(betResultRetryLogService.calculateNextRetryTime(rawBetResultRetryLogItem.getRetryCounter(), rawBetResultRetryLogItem.getNextRetryTime()));

                if (rawBetResultRetryLogItem.getRetryCounter() > maxRetryCounter) {
                    rawBetResultRetryLogItem.setStatus(RetryStatus.TIMEOUT.code);
                }

            }
            rawBetResultRetryLogRepository.save(rawBetResultRetryLogItem);

        }

    }
}
