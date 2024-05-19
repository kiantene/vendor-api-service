package com.nextgen.gameaggregator.scheduler;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.RawBetResultRetryLog;
import com.nextgen.gameaggregator.enums.RetryStatus;
import com.nextgen.gameaggregator.repository.ga.writer.RawBetResultRetryLogRepository;
import com.nextgen.gameaggregator.service.BetResultRetryLogService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${scheduler.bet-result-retry:false}")
    private Boolean useScheduler;

    @Scheduled(fixedDelay = 3000, initialDelay = 3000)
    public void processBetResultRetryLog() {

        Long currentTime = System.currentTimeMillis() + 1;
        Integer maxRetryCounter = 7;
        Integer status = RetryStatus.FAILED.code;
        GeneralVo vo = new GeneralVo();

        if (!useScheduler) {
            //Do nothing
        } else {

            //get maximum 10 records once per query
            List<RawBetResultRetryLog> rawBetResultRetryLogList = rawBetResultRetryLogRepository.findByNextRetryTimeLessThanAndRetryCounterLessThanAndStatusEquals(currentTime, maxRetryCounter, status);

            if (!rawBetResultRetryLogList.isEmpty()) {
                for (RawBetResultRetryLog rawBetResultRetryLogItem : rawBetResultRetryLogList) {
                    HttpRequestLog httpRequestLog = httpService.startRetryRequestToOperator(rawBetResultRetryLogItem);

                    try {
                        betResultRetryLogService.call(rawBetResultRetryLogItem.getOperatorData(), rawBetResultRetryLogItem.getAction(), rawBetResultRetryLogItem.getAgentId(), httpRequestLog);
                        rawBetResultRetryLogItem.setStatus(RetryStatus.SUCCESS.code);
                        vo.setResponseCode(ResponseCode.SUCCESS);

                    } catch (Exception e) {
                        httpService.logError(httpRequestLog, e);
                        vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);

                        rawBetResultRetryLogItem.setStatus(RetryStatus.FAILED.code);
                        rawBetResultRetryLogItem.setRetryCounter(rawBetResultRetryLogItem.getRetryCounter() + 1);
                        rawBetResultRetryLogItem.setNextRetryTime(betResultRetryLogService.calculateNextRetryTime(rawBetResultRetryLogItem.getRetryCounter(), currentTime));

                        if (rawBetResultRetryLogItem.getRetryCounter() > maxRetryCounter) {
                            rawBetResultRetryLogItem.setStatus(RetryStatus.TIMEOUT.code);
                        }

                    } finally {
                        httpService.end(httpRequestLog, vo);

                    }
                    rawBetResultRetryLogRepository.save(rawBetResultRetryLogItem);

                }
            }
        }
    }
}
