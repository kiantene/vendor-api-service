package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.entity.ga.custom.WarehouseFutureEntity;
import com.nextgen.gameaggregator.entity.wallet.TransferHistory;
import com.nextgen.gameaggregator.entity.warehouse.PromoPayoutHistory;
import com.nextgen.gameaggregator.logging.ApiRequestLog;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.service.kafka.KafkaDlqService;
import com.nextgen.gameaggregator.service.kafka.KafkaSerializerType;
import com.nextgen.gameaggregator.service.data.producer.endround.RoundEndedTriggerMessage;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import com.nextgen.gameaggregator.util.StackTraceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KafkaService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Gson GSON = new Gson();
    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final KafkaTemplate<String, Object> jsonSchemaKafkaTemplate;
    private final KafkaTemplate<String, Object> apiRequestLogKafkaTemplate;
    private final CurrencyConversionService currencyConversionService;
    private final WarehouseBetHistoryService warehouseBetHistoryService;
    private final AgentPlayerService agentPlayerService;
    private final VendorPlayerService vendorPlayerService;
    private final S3BetService s3BetService;
    private final KafkaDlqService kafkaDlqService;

    @Value("${logging.log-to-kafka:true}")
    private boolean logToKafka;

    @Autowired
    public KafkaService(KafkaTemplate<String, String> stringKafkaTemplate,
                        @Qualifier("jsonSchemaKafkaTemplate") KafkaTemplate<String, Object> jsonSchemaKafkaTemplate,
                        @Qualifier("apiRequestLogKafkaTemplate") KafkaTemplate<String, Object> apiRequestLogKafkaTemplate,
                        CurrencyConversionService currencyConversionService,
                        WarehouseBetHistoryService warehouseBetHistoryService,
                        AgentPlayerService agentPlayerService,
                        VendorPlayerService vendorPlayerService,
                        S3BetService s3BetService,
                        KafkaDlqService kafkaDlqService
    ) {

        this.stringKafkaTemplate = stringKafkaTemplate;
        this.jsonSchemaKafkaTemplate = jsonSchemaKafkaTemplate;
        this.apiRequestLogKafkaTemplate = apiRequestLogKafkaTemplate;
        this.currencyConversionService = currencyConversionService;
        this.warehouseBetHistoryService = warehouseBetHistoryService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
        this.s3BetService = s3BetService;
        this.kafkaDlqService = kafkaDlqService;
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
            log.error("BetHistory roundId=[{}], {}, {}", betHistory.getRoundId(), e.getMessage(), StackTraceUtils.getStackTraceAsString(e));
        }
    }

    public void produceOperatorRequestDlq(BetHistory betHistory, BigDecimal conversionRate, String vendorPlayerUsername) {
        try {
            //will do currency conversion before send to kafka
            currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(betHistory, conversionRate);
            jsonSchemaKafkaTemplate.send(KafkaConstant.OPERATOR_REQUEST_DLQ, vendorPlayerUsername, betHistory);

        } catch (Exception e) {
            log.error(e.getMessage() + " -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
            log.error("OperatorRequestDlq roundId=[{}], {}, {}", betHistory.getRoundId(), e.getMessage(), StackTraceUtils.getStackTraceAsString(e));
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
            log.error("BetResultDlq roundId=[{}], {}, {}", msg.getRoundId(), e.getMessage(), StackTraceUtils.getStackTraceAsString(e));
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
            log.error("WarehouseBetHistory roundId=[{}], {}, {}", betHistory.getRoundId(), e.getMessage(), StackTraceUtils.getStackTraceAsString(e));
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

            // GA-14578: send with durable fallback (keyless — preserve current partitioning).
            String dedupKey = betHistory.getVendorId() + "::" + betHistory.getId() + "::" + betHistory.getRoundId();
            kafkaDlqService.sendWithFallback(KafkaConstant.TOPIC_BET_HISTORY_PREPROCESSING_V3, null, dedupKey,
                    betHistoryV3, KafkaSerializerType.JSON_SCHEMA);

        } catch (Exception e) {
            log.error("PreprocessingBetHistoryV3: " + e.getMessage() + " -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
            log.error("PreprocessBetHistory vendorBetId=[{}], {}, {}", betHistory.getVendorBetId(), e.getMessage(), StackTraceUtils.getStackTraceAsString(e));
        }
    }

    public void produceUnsettledBet(SportUnsettledBetMariaDB sportUnsettledBetMariaDB) {
        try {
            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_UNSETTLED_BET, sportUnsettledBetMariaDB);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void producePromoPayoutHistory(PromoPayoutHistory promoPayoutHistory) {
        try {
            stringKafkaTemplate.send(KafkaConstant.TOPIC_PROMO_PAYOUT_HISTORY, promoPayoutHistory.getVendorPlayerUsername(), OBJECT_MAPPER.writeValueAsString(promoPayoutHistory));
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
            log.error("TransferHistory id=[{}], {}, {}", transferHistory.getId(), e.getMessage(), StackTraceUtils.getStackTraceAsString(e));
        }
    }

    public void produceApiRequestLog(ApiRequestLog apiRequestLog) {
        if (this.logToKafka) {
            try {
                apiRequestLogKafkaTemplate
                        .send(KafkaConstant.TOPIC_API_REQUEST_LOG, apiRequestLog.getUsername(), apiRequestLog)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("ApiRequestLog delivery failed roundId=[{}], {}, {}", apiRequestLog.getRoundId(), ex.getMessage(), StackTraceUtils.getStackTraceAsString(ex));
                                log.info(GSON.toJson(apiRequestLog));
                            }
                        });
            } catch (Exception e) {
                log.error("ApiRequestLog delivery failed roundId=[{}], {}, {}", apiRequestLog.getRoundId(), e.getMessage(), StackTraceUtils.getStackTraceAsString(e));
                log.info(GSON.toJson(apiRequestLog));
            }
        } else {
            log.info(GSON.toJson(apiRequestLog));
        }
    }

    public void produceBetTransactionLog(BetInformation betInformation, BetResultData betResultData, String vendorPlayerUsername) {
        try {
            BetTransactionLog betTransactionLog;
            if (betResultData != null) {
                betTransactionLog = new BetTransactionLog(betInformation, betResultData);
            } else {
                // set back to original data
                betTransactionLog = new BetTransactionLog(betInformation);
            }

            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_BET_TRANSACTION_LOG, vendorPlayerUsername, betTransactionLog);
        } catch (Exception e) {
            log.error("BetTransactionLog: " + e.getMessage() + " -> vendorBetId = " + betInformation.getVendorBetId() + "& roundId = " + betInformation.getRoundId());
            log.error("BetTransactionLog roundId=[{}], {}, {}", betInformation.getRoundId(), e.getMessage(), StackTraceUtils.getStackTraceAsString(e));
        }
    }

    private WarehouseFutureEntity getFutureEntityForBetHistory(BetHistory betHistory) {
        return warehouseBetHistoryService.getWarehouseBetHistoryInfoCache(betHistory.getVendorGameId(), betHistory.getVendorId(), betHistory.getGameCategoryId(),
                betHistory.getCurrencyId(), betHistory.getAgentId());
    }

    public void produceBetHistoryV3(BetHistoryV3 betHistoryV3) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(betHistoryV3);

            // GA-14578: send with durable fallback (keyless — preserve current partitioning).
            String dedupKey = betHistoryV3.getVendorId() + "::" + betHistoryV3.getId() + "::" + betHistoryV3.getRoundId();
            kafkaDlqService.sendWithFallback(KafkaConstant.TOPIC_BET_HISTORY_V4, null, dedupKey,
                    json, KafkaSerializerType.STRING);
        } catch (Exception ex) {
            log.error("BetHistoryV3: " + ex.getMessage() + " -> vendorBetId = " + betHistoryV3.getVendorBetId() + "& roundId = " + betHistoryV3.getRoundId());
            log.error("BetHistoryV3 roundId=[{}], {}", betHistoryV3.getRoundId(), ex.getMessage(), ex);
        }
    }

    public void produceBetHistoryV3(BetHistory betHistory, String productCode, Integer productId, Integer productGameId, String agentPlayerUsername, String vendorPlayerUsername, BigDecimal conversionRate) {
        try {
            //will do currency conversion before send to kafka
            currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(betHistory, conversionRate);

            if (betHistory.getGameSessionToken() == null) {
                betHistory.setGameSessionToken("");
            }

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

            // GA-14578: send with durable fallback (keyless — preserve current partitioning).
            String dedupKey = betHistory.getVendorId() + "::" + betHistory.getId() + "::" + betHistory.getRoundId();
            kafkaDlqService.sendWithFallback(KafkaConstant.TOPIC_BET_HISTORY_V4, null, dedupKey,
                    json, KafkaSerializerType.STRING);

        } catch (Exception e) {
            log.error("BetHistoryV3: " + e.getMessage() + " -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
            log.error("BetHistoryV3 roundId=[{}], {}", betHistory.getRoundId(), e.getMessage(), e);
        }
    }

    public void produceBetHistoryUncap(BetHistoryUncap betHistoryUncap) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(betHistoryUncap);

            CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send(KafkaConstant.TOPIC_BET_HISTORY_UNCAP, json);

            future.exceptionally(throwable -> {
                log.error("Error sending BetHistoryUncap to Kafka: ", throwable);
                return null;
            });

        } catch (Exception e) {
            log.error("BetHistoryUncap: " + e.getMessage() + " -> vendorBetId = " + betHistoryUncap.getVendorBetId() + "& roundId = " + betHistoryUncap.getRoundId());
        }
    }

    public void produceSportResettleDateChange(BetHistory betHistory) {
        try {
            CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send(KafkaConstant.TOPIC_RESETTLEMENT_DATE_CHANGE, new Gson().toJson(betHistory));

            future.exceptionally(throwable -> {
                log.error("Error sending resettlement date change to Kafka: ", throwable);
                return null;
            });

        } catch (Exception e) {
            log.error("Resettlement Date Changes: " + e.getMessage() + " -> vendorBetId = " + betHistory.getVendorBetId() + "& roundId = " + betHistory.getRoundId());
        }
    }

    public void produceSportRefundPatchingDLQ(SportRefundBetPatching sportRefundBetPatching) {
        try {
            stringKafkaTemplate.send(KafkaConstant.TOPIC_PATCHING_SPORT_UNSETTLED_BET_TO_REFUND_BET_DLQ, new Gson().toJson(sportRefundBetPatching));
        } catch (Exception e) {
            log.error("SportRefundPatchingDLQ: " + e.getMessage() + " -> vendorBetId = " + sportRefundBetPatching.getVendorBetId() + "& roundId = " + sportRefundBetPatching.getRoundId());
        }
    }

    public void produceSettledBetDlq(SettledBet settledBet) {
        try {
            jsonSchemaKafkaTemplate.send(KafkaConstant.TOPIC_SETTLED_BET_DLQ, settledBet.getRoundId(), settledBet);

        } catch (Exception e) {
            log.error("produceSettledBetDlq failed : " + e.getMessage() + " -> vendorBetId = " + settledBet.getVendorBetId() + "& roundId = " + settledBet.getRoundId());
            e.printStackTrace();
        }
    }

    public void produceBetTransactionHistory(BetTransactionHistory betTransactionHistory) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(betTransactionHistory);

            CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send(KafkaConstant.TOPIC_BET_TRANSACTION_HISTORY, betTransactionHistory.getVendorPlayerUsername(), json);

            future.exceptionally(throwable -> {
                log.error("Error sending TransactionHistory to Kafka: ", throwable);
                return null;
            });
        } catch (Exception ex) {
            log.error(ex.getMessage() + " : " + betTransactionHistory.toString());
        }
    }

    public void produceRoundEndedTrigger(RoundEndedTriggerMessage message, String username) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(message);

            // To use Operator Username as Kafka Key
            CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send(KafkaConstant.TOPIC_FRAMEWORK_V2_ROUND_ENDED_TRIGGER, username, json);

            future.exceptionally(throwable -> {
                log.error("Error sending topic_framework_v2_round_ended_trigger to Kafka: ", throwable);
                return null;
            });
        } catch (Exception ex) {
            log.error(ex.getMessage() + " : " + message.toString() + " : " + username);
        }
    }
}
