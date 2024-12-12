package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.entity.ga.custom.WarehouseFutureEntity;
import com.nextgen.gameaggregator.entity.wallet.TransferHistory;
import com.nextgen.gameaggregator.logging.ApiRequestLog;
import com.nextgen.gameaggregator.logging.TransferWalletRequestLog;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class KafkaService {

    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final KafkaTemplate<String, Object> jsonSchemaKafkaTemplate;
    private final CurrencyConversionService currencyConversionService;
    private final WarehouseBetHistoryService warehouseBetHistoryService;
    private final AgentPlayerService agentPlayerService;
    private final VendorPlayerService vendorPlayerService;
    private final S3BetService s3BetService;
    private final VendorService vendorService;
    private final VendorGameCodeService vendorGameCodeService;
    private final AgentService agentService;

    @Value("${logging.log-to-kafka:true}")
    private boolean logToKafka;


    @Autowired
    public KafkaService(KafkaTemplate<String, String> stringKafkaTemplate,
                        KafkaTemplate<String, Object> jsonSchemaKafkaTemplate,
                        CurrencyConversionService currencyConversionService,
                        WarehouseBetHistoryService warehouseBetHistoryService,
                        AgentPlayerService agentPlayerService,
                        VendorPlayerService vendorPlayerService,
                        CachingService cachingService,
                        S3BetService s3BetService,
                        VendorService vendorService,
                        VendorGameCodeService vendorGameCodeService,
                        AgentService agentService
    ) {

        this.stringKafkaTemplate = stringKafkaTemplate;
        this.jsonSchemaKafkaTemplate = jsonSchemaKafkaTemplate;
        this.currencyConversionService = currencyConversionService;
        this.warehouseBetHistoryService = warehouseBetHistoryService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
        this.s3BetService = s3BetService;
        this.vendorService = vendorService;
        this.vendorGameCodeService = vendorGameCodeService;
        this.agentService = agentService;
    }

    public void produceBetHistory(BetHistory betHistory, String vendorPlayerUsername, BigDecimal conversionRate) {
        try {
            //will do currency conversion before send to kafka
            currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(betHistory, conversionRate);

            if (betHistory.getGameSessionToken() == null) {
                betHistory.setGameSessionToken("");
            }

            if (vendorPlayerUsername == null) {
                vendorPlayerUsername = "";
            }

            //NOV 7, disable sending bet data to topic_bet_history_v2, due to no longer using mariaDB, using clickhouse now
            //jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_BET_HISTORY_V2, vendorPlayerUsername, betHistory);

            //NOV 7, disable sending bet data to topic_bet_history_v2, due to no longer using mariaDB, using clickhouse now
            //jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_BET_HISTORY_V2, vendorPlayerUsername, betHistory);

        } catch (Exception e) {
            log.error(e.getMessage() + " -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
            e.printStackTrace();
        }
    }

    public void produceOperatorRequestDlq(BetHistory betHistory, BigDecimal conversionRate, String vendorPlayerUsername) {
        try {
            //will do currency conversion before send to kafka
            currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(betHistory, conversionRate);
            jsonSchemaKafkaTemplate.send(KafkaConstant.OPERATOR_REQUEST_DLQ, vendorPlayerUsername, betHistory);

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
            //currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(betHistory, conversionRate);
            WarehouseFutureEntity warehouseFutureEntity =
                    warehouseBetHistoryService.getWarehouseBetHistoryInfoCache(
                            betHistory.getVendorGameId(), betHistory.getVendorId(),
                            betHistory.getGameCategoryId(), betHistory.getCurrencyId(), betHistory.getAgentId());


            if (agentPlayerUsername == null || agentPlayerUsername.isEmpty()) {
                AgentPlayer agentPlayer = agentPlayerService.get(betHistory.getAgentPlayerId());
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

            stringKafkaTemplate.send(KafkaConstant.TOPIC_WAREHOUSE_BET_HISTORY, mapper.writeValueAsString(warehouseBetHistory));

            s3BetService.uploadBetHistoryJsonFileAsync(warehouseBetHistory);

            if (warehouseBetHistoryService.checkIsDelaySettlement(warehouseBetHistory)) {
                stringKafkaTemplate.send(KafkaConstant.TOPIC_BET_HISTORY_DELAY_SETTLEMENT, mapper.writeValueAsString(warehouseBetHistory));
            }

        } catch (Exception e) {
            log.error(e.getMessage() + " -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
            e.printStackTrace();
        }
    }

    public void producePreprocessingBetHistory(BetHistory betHistory, String agentPlayerUsername, String vendorPlayerUsername, BigDecimal conversionRate) {

        try {
            System.err.println("SEND TO " + KafkaConstant.TOPIC_BET_HISTORY_PREPROCESSING_V3);
            //will do currency conversion before send to kafka
            currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(betHistory, conversionRate);

            WarehouseFutureEntity warehouseFutureEntity =
                    warehouseBetHistoryService.getWarehouseBetHistoryInfoCache(
                            betHistory.getVendorGameId(), betHistory.getVendorId(),
                            betHistory.getGameCategoryId(), betHistory.getCurrencyId(), betHistory.getAgentId());


            if (agentPlayerUsername == null || agentPlayerUsername.isEmpty()) {
                AgentPlayer agentPlayer = agentPlayerService.get(betHistory.getAgentPlayerId());
                agentPlayerUsername = agentPlayer.getUsername();
                log.error("PreprocessingBetHistoryV3-agentPlayerUsername is empty detail:" + new Gson().toJson(betHistory));
            }

            if (vendorPlayerUsername == null || vendorPlayerUsername.isEmpty()) {
                VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(betHistory.getVendorPlayerId(), null);
                vendorPlayerUsername = vendorPlayer.getUsername();
                log.error("PreprocessingBetHistoryV3-vendorPlayerUsername is empty detail:" + new Gson().toJson(betHistory));
            }

            BetHistoryV3 betHistoryV3 = new BetHistoryV3(betHistory, null, null, null, agentPlayerUsername,
            vendorPlayerUsername, warehouseFutureEntity);

            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_BET_HISTORY_PREPROCESSING_V3, betHistoryV3);

        } catch (Exception e) {
            log.error("PreprocessingBetHistoryV3: " + e.getMessage() + " -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
            e.printStackTrace();
        }
    }


    public void produceEndRoundSettleBet(EndRoundSettledBet endRoundSettledBet) {
        try {
            //updated 20 May 2024, from TOPIC_END_ROUND_PROCESS to TOPIC_END_ROUND_PROCESS_V2 for partitioning production data purposes
            CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send(KafkaConstant.TOPIC_END_ROUND_PROCESS_V2, new Gson().toJson(endRoundSettledBet));
            future.orTimeout(5, TimeUnit.SECONDS).exceptionally(throwable -> {
                // Return a default value if needed
                if (throwable instanceof java.util.concurrent.TimeoutException) {
                    // Handle timeout scenario
                    log.error("FunctionName: produceEndRoundSettleBet (Throwable) Timeout: No response after 5 seconds");
                } else {
                    // Handle failure
                    log.error("FunctionName: produceEndRoundSettleBet (Throwable) | {} | {} | {}",
                            "TraceId: " + endRoundSettledBet.getId(),
                            "RoundId: " + endRoundSettledBet.getRoundId(),
                            "Error: " + throwable.toString());
                }
                return null; // Return a default value
            });
        } catch (Exception e) {
            log.error("FunctionName: produceEndRoundSettleBet (Exception) | {} | {} | {}",
                    "TraceId: " + endRoundSettledBet.getId(),
                    "RoundId: " + endRoundSettledBet.getRoundId(),
                    "Error: " + e);
        }
    }

    public void produceUnsettledBet(SportUnsettledBetMariaDB sportUnsettledBetMariaDB) {
        try {
            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_UNSETTLED_BET, sportUnsettledBetMariaDB);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void produceUnsettledBet(SportUnsettledBetMariaDB sportUnsettledBetMariaDB, BigDecimal conversionRate) {
        try {
            sportUnsettledBetMariaDB.setBetAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(sportUnsettledBetMariaDB.getBetAmount(), conversionRate));
            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_UNSETTLED_BET, sportUnsettledBetMariaDB);

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void produceMasterUnsettledBet(SportMasterUnsettledBetMariaDB sportMasterUnsettledBetMariaDB, BigDecimal conversionRate) {
        try {
            sportMasterUnsettledBetMariaDB.setBetAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(sportMasterUnsettledBetMariaDB.getBetAmount(), conversionRate));
            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_MASTER_UNSETTLED_BET, sportMasterUnsettledBetMariaDB);

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

    public void produceApiRequestLog(ApiRequestLog apiRequestLog) {
        if (this.logToKafka) {
            try {
                jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_API_REQUEST_LOG, apiRequestLog.getUsername(), apiRequestLog);
            } catch (Exception e) {
                log.error("[" + apiRequestLog.getRoundId() + "] " + e.getMessage());
                log.info(new Gson().toJson(apiRequestLog));
            }
        } else {
            log.info(new Gson().toJson(apiRequestLog));
        }
    }

    public void produceTransferWalletRequestLog(TransferWalletRequestLog transferWalletRequestLog) {
        if (this.logToKafka) {
            try {
                jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_TRANSFER_WALLET_REQUEST_LOG, transferWalletRequestLog.getUsername(), transferWalletRequestLog);
            } catch (Exception e) {
                log.error(transferWalletRequestLog.getTraceId() + " : " + e.getMessage());
                log.info(new Gson().toJson(transferWalletRequestLog));
            }
        } else {
            log.info(new Gson().toJson(transferWalletRequestLog));
        }
    }

    private WarehouseFutureEntity getFutureEntityForBetHistory(BetHistory betHistory) {
        return warehouseBetHistoryService.getWarehouseBetHistoryInfoCache(betHistory.getVendorGameId(), betHistory.getVendorId(), betHistory.getGameCategoryId(),
            betHistory.getCurrencyId(), betHistory.getAgentId());
    }

    public void produceBetHistoryV3(BetHistory betHistory, String productCode, Integer productId, Integer productGameId, String agentPlayerUsername, String vendorPlayerUsername) {
        try {
            // TODO : Will re-enable after the new game list import is deployed
            // if (productId == null || productCode == null) {
            //     Vendor vendor = vendorService.getById(betHistory.getVendorId());
            //     productId = vendor.getProduct().getId();
            //     productCode = vendor.getProduct().getCode();
            // }

            // if (productGameId == null) {
            //     VendorGameCode vendorGameCode = vendorGameCodeService.getByTop1VendorGameId(betHistory.getVendorGameId());
            //     productGameId = vendorGameCode.getProductGameId();
            // }

            if (agentPlayerUsername == null) {
                AgentPlayer agentPlayer = agentPlayerService.get(betHistory.getAgentPlayerId());
                agentPlayerUsername = agentPlayer.getUsername();
            }

            if (vendorPlayerUsername == null) {
                VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(betHistory.getVendorPlayerId(), null);
                vendorPlayerUsername = vendorPlayer.getUsername();
            }

            WarehouseFutureEntity warehouseFutureEntity = this.getFutureEntityForBetHistory(betHistory);
            BetHistoryV3 betHistoryV3 = new BetHistoryV3(betHistory, productCode, productId, productGameId, agentPlayerUsername,
                    vendorPlayerUsername, warehouseFutureEntity);

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(betHistoryV3);

            CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send(KafkaConstant.TOPIC_BET_HISTORY_V3, json);

            future.exceptionally(throwable -> {
                log.error("Error sending BetHistoryV3 to Kafka: ", throwable);
                return null;
            });

        } catch (Exception e) {
            log.error("BetHistoryV3: " + e.getMessage() + " -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
        }
    }
}