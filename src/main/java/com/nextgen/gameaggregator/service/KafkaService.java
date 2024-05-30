package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.entity.ga.custom.WarehouseFutureEntity;
import com.nextgen.gameaggregator.entity.wallet.TransferHistory;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
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
    @Autowired
    private WarehouseBetHistoryService warehouseBetHistoryService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorPlayerService vendorPlayerService;

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

    public void produceBetResultDlq(BetResultData betResultData, GameSession gameSession, HttpRequestLog httpRequestLog) {
        Integer vendorGameId = gameSession.getVendorGameId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        Integer agentId = gameSession.getAgentId();

        BetResultDlq msg = new BetResultDlq(betResultData);
        msg.setVendorId(gameSession.getVendorId());
        msg.setVendorGameId(vendorGameId);
        msg.setVendorPlayerId(vendorPlayerId);
        msg.setAgentId(agentId);
        msg.setAgentPlayerId(gameSession.getAgentPlayerId());
        msg.setGameCategoryId(gameSession.getGameCategoryId());
        msg.setCurrencyId(gameSession.getCurrencyId());
        msg.setGameSessionToken(gameSession.getToken());
        msg.setRequestTime(httpRequestLog.getStartTime());

        try {
            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_BET_RESULT_DLQ, msg);

        } catch (Exception e) {
            log.error(e.getMessage() + " -> BetResultData = " + betResultData + " -> vendorGameId = " + vendorGameId + " -> roundId = " + msg.getRoundId() + " -> vendorPlayerId = " + vendorPlayerId + " -> agentId = " + agentId);
            e.printStackTrace();
        }
    }

    public void produceWarehouseBetHistory(BetHistory betHistory, String agentPlayerUsername, String vendorPlayerUsername, BigDecimal conversionRate) {
        try {
            //will do currency conversion before send to kafka
            currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(betHistory, conversionRate);
            WarehouseFutureEntity warehouseFutureEntity =
                    warehouseBetHistoryService.getWarehouseBetHistoryInfoCache(
                            betHistory.getVendorGameId(), betHistory.getVendorId(),
                            betHistory.getGameCategoryId(), betHistory.getCurrencyId());


            if (agentPlayerUsername == null || agentPlayerUsername.isEmpty()) {
                AgentPlayer agentPlayer = agentPlayerService.getByAgentPlayerId(betHistory.getAgentPlayerId(), null);
                agentPlayerUsername = agentPlayer.getUsername();
                        log.error("WarehouseBetHistory-agentPlayerUsername is empty detail:" + new Gson().toJson(betHistory));
            }

            if (vendorPlayerUsername == null || vendorPlayerUsername.isEmpty()) {
                VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(betHistory.getVendorPlayerId(), null);
                vendorPlayerUsername = vendorPlayer.getUsername();
                log.error("WarehouseBetHistory-vendorPlayerUsername is empty detail:" + new Gson().toJson(betHistory));
            }

            com.nextgen.gameaggregator.entity.warehouse.BetHistory warehouseBetHistory
                    = new com.nextgen.gameaggregator.entity.warehouse.BetHistory
                    (betHistory, agentPlayerUsername, vendorPlayerUsername, warehouseFutureEntity);

            // Using Jackson or any other JSON library to convert UserData object to JSON string
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = "{}";

            stringKafkaTemplate.send(KafkaConstant.TOPIC_WAREHOUSE_BET_HISTORY, mapper.writeValueAsString(warehouseBetHistory));
            //ga-1726 temporary remove delete actions
            //settledBetService.delete(settledBet);
        } catch (Exception e) {
            log.error(e.getMessage() + " -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
            e.printStackTrace();
        }
    }

    public void producePreprocessingBetHistory(BetHistory betHistory, SettledBet settledBet, BigDecimal conversionRate) {
        try {
            System.err.println("SEND TO " + KafkaConstant.TOPIC_BET_HISTORY_PREPROCESSING);
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
            //updated 20 May 2024, from TOPIC_END_ROUND_PROCESS to TOPIC_END_ROUND_PROCESS_V2 for partitioning production data purposes
            stringKafkaTemplate.send(KafkaConstant.TOPIC_END_ROUND_PROCESS_V2, new Gson().toJson(endRoundSettledBet));
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

    public void produceUnsettledBet(VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB, BigDecimal conversionRate) {
        try {
            sportUnsettledBetMariaDB.setBetAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(sportUnsettledBetMariaDB.getBetAmount(), conversionRate));
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

    public void produceTransferHistory(TransferHistory transferHistory) {
        try {

            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_TRANSFER_HISTORY, transferHistory);

        } catch (Exception e) {
            log.error(e.getMessage() + " -> referenceId = " + transferHistory.getId() + " data : " + new Gson().toJson(transferHistory));
            e.printStackTrace();
        }
    }

    public void produceHttpResponseLog(HttpResponseLog httpResponseLog) {
        try {
            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_HTTP_RESPONSE_LOG, httpResponseLog);
        } catch (Exception e) {
            log.error(e.getMessage() + " produceHttpResponseLog[" + httpResponseLog.getId() + "]");
            e.printStackTrace();
        }
    }
}