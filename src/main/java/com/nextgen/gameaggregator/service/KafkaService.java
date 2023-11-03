package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.EndRoundSettledBet;
import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetMariaDB;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class KafkaService {

    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;
    @Autowired
    private KafkaTemplate<String, Object> jsonSchemaKafkaTemplate;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private CurrencyConversionService currencyConversionService;

    public void produceBetHistory(BetHistory betHistory, SettledBet settledBet, BigDecimal conversionRate) {
        try {
            //will do currency conversion before send to kafka
            currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(betHistory, conversionRate);

            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_BET_HISTORY, betHistory);
            //ga-1726 temporary remove delete actions
            //settledBetService.delete(settledBet);
        } catch (Exception e) {
            log.error(e.getMessage() + " -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
            e.printStackTrace();
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

    public void produceUnsettledBet(SportUnsettledBetMariaDB sportUnsettledBetMariaDB) {
        try {
            stringKafkaTemplate.send(KafkaConstant.TOPIC_UNSETTLED_BET, new Gson().toJson(sportUnsettledBetMariaDB));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void produceSettledBet(SportSettledBet sportSettledBet) {
        try {
            stringKafkaTemplate.send(KafkaConstant.TOPIC_SETTLED_BET, new Gson().toJson(sportSettledBet));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
