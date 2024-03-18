package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.EndRoundSettledBet;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
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

            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_BET_HISTORY_V2, betHistory);
            //ga-1726 temporary remove delete actions
            //settledBetService.delete(settledBet);
        } catch (Exception e) {
            log.error(e.getMessage() + " -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
            e.printStackTrace();
        }
    }

    public void producePreprocessingBetHistory(BetHistory betHistory, SettledBet settledBet, BigDecimal conversionRate) {
        try {
            System.err.println("SEND TO "+ KafkaConstant.TOPIC_BET_HISTORY_PREPROCESSING);
            //will do currency conversion before send to kafka
            currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(betHistory, conversionRate);

            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_BET_HISTORY_PREPROCESSING, betHistory);

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

    public void produceUnsettledBet(VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB) {
        try {
            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_UNSETTLED_BET, sportUnsettledBetMariaDB);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void produceRawSettledBet(SportRawSettledBet sportRawSettledBet) {
        try {
            stringKafkaTemplate.send(KafkaConstant.TOPIC_RAW_SETTLED_BET, new Gson().toJson(sportRawSettledBet));

        } catch (Exception e) {
            log.error(e.getMessage());

        }
    }
}
