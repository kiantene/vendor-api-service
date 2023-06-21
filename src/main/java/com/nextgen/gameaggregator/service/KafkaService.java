package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.*;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.SettledBet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaService {

    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;
    @Autowired
    private KafkaTemplate<String, Object> jsonSchemaKafkaTemplate;
    @Autowired
    private SettledBetService settledBetService;

    public void produceBetHistory(BetHistory betHistory, SettledBet settledBet) {
        try {
            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_BET_HISTORY, betHistory);
            //ga-1726 temporary remove delete actions
            //settledBetService.delete(settledBet);
        } catch (Exception e) {
            log.warn("Kafka produceBetHistory.exception -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
        }
    }

    public void produceEndRoundSettleBet(EndRoundSettledBet endRoundSettledBet) {
        try {
            stringKafkaTemplate.send(KafkaConstant.TOPIC_END_ROUND_PROCESS, new Gson().toJson(endRoundSettledBet));
        } catch (Exception e) {
            //log.warn(KafkaConstant.TOPIC_END_ROUND_PROCESS + " | Kafka produceBetHistory.exception -> vendorBetId = " + endRoundBetHistory.getVendorBetId() + "& roundId = " + endRoundBetHistory.getRoundId());
            log.error(e.getMessage());
        }
    }

    public void send(HttpRequestLog httpRequestLog) {
        jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_HTTP_REQUEST_LOGS, httpRequestLog);
    }
}
