package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultAction;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.api.cancelbet.CancelBetDto;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class KafkaConsumerService {
    private static final Integer THREAD_SIZE = 64;
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);

    @Autowired
    private WalletBetResultAction walletBetResultAction;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private RequestService requestService;
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private LoggingService loggingService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private SportWalletService sportWalletService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorPlayerService vendorPlayerService;

    @KafkaListener(topics = KafkaConstant.TOPIC_END_ROUND_PROCESS_V2, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeEndRoundProcessV2(String message) throws RecordNotFoundException, InvalidPlayerException {

        //prepare endRoundProcess Log
        Exception exception = null;
        String newTraceId = UUID.randomUUID().toString();
        HttpRequestLog httpRequestLog = httpService.startEndRoundConsumerLog();

        ProcessEndRoundLog processEndRoundLog = new ProcessEndRoundLog();
        processEndRoundLog.setStartTime(System.currentTimeMillis());
        processEndRoundLog.setTraceId(newTraceId);

        //prepare endRound and settleBet info
        EndRoundSettledBet endRoundSettledBet = new Gson().fromJson(message, EndRoundSettledBet.class);
        endRoundSettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
        SettledBet settledBet = new SettledBet(endRoundSettledBet);
        settledBet.setResultType(endRoundSettledBet.getGaResultType());

        processEndRoundLog.setRawBody(endRoundSettledBet.getRawData());
        processEndRoundLog.setRoundId(settledBet.getRoundId());
        processEndRoundLog.setVendorBetId(settledBet.getVendorBetId());

        try {
            httpRequestLog.setBetStart(System.currentTimeMillis());

            AgentPlayer agentPlayer = agentPlayerService.get(endRoundSettledBet.getAgentPlayerId());
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(endRoundSettledBet.getVendorPlayerId(), null);

            //get is bet = sidebet
            vendorService.verifyIsPreProcessingVendorGame(endRoundSettledBet.getVendorGameId());

            //get vendorCurrencyRate for the vendor
            GameSession gameSession = new GameSession(endRoundSettledBet);
            VendorCurrency vendorCurrency = vendorService.getCurrencyConversionRate(gameSession, newTraceId);

            //update settledBet info
            settledBetService.save(settledBet, settledBet.getRawData());

            //prepare insert new betHistory data
            BetHistory betHistory = new BetHistory(settledBet);
            if (!vendorService.getBetPreprocess().getIsPreProcessBet()) {
                // process bet as normal bet and send to kafka topic_bet_history topic
                kafkaService.produceBetHistory(betHistory, gameSession.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());
                // process bet as normal bet and send to kafka topic_warehouse_bet_history topic
                // kafkaService.produceWarehouseBetHistory
                //         (betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
                kafkaService.produceBetHistoryV3(betHistory, gameSession.getProductCode(), gameSession.getProductId(), gameSession.getProductGameId(),
                        gameSession.getAgentPlayerUsername(), gameSession.getVendorPlayerUsername());
            } else {
                // process bet as preprocessing bet and send to kafka topic_bet_history_preprocessing topic
                kafkaService.producePreprocessingBetHistory(betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
            }

            //prepare delete unsettledBet
            UnsettledBet unsettledBet = new UnsettledBet(settledBet);
            unsettledBetService.delete(unsettledBet);

            //prepare and send endRound to operator
            this.notifyEndRoundProcess(newTraceId, agentPlayer, vendorPlayer, gameSession, settledBet, vendorCurrency.getFromVendorRate(), vendorCurrency.getToVendorRate(), endRoundSettledBet, settledBet, httpRequestLog);

        } catch (GameNotSupportedException e) {
            exception = e;

        } catch (VendorCurrencyNotSupportException e) {
            exception = e;

        } catch (Exception e) {
            exception = e;

        } finally {
            if (exception != null) {
                //prepare and save processEndRoundLog if exception not null;
                processEndRoundLog.setEndTime(System.currentTimeMillis());
                RequestService.processEndRoundLog(processEndRoundLog, exception, endRoundSettledBet);
            }
        }
    }

    @KafkaListener(topics = KafkaConstant.TOPIC_END_ROUND_PROCESS_V3, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeEndRoundProcessV3(String message) {

        //prepare endRoundProcess Log
        Exception exception = null;
        ObjectMapper objectMapper = new ObjectMapper();
        EndRoundSettledBetForPatching endRoundSettledBetForPatching = null;
        String newTraceId = UUID.randomUUID().toString();
        HttpRequestLog httpRequestLog = httpService.startEndRoundConsumerLog();

        ProcessEndRoundLog processEndRoundLog = new ProcessEndRoundLog();
        processEndRoundLog.setStartTime(System.currentTimeMillis());
        processEndRoundLog.setTraceId(newTraceId);

        try {
            //prepare endRound and settleBet info
            endRoundSettledBetForPatching = objectMapper.readValue(message, EndRoundSettledBetForPatching.class);
            endRoundSettledBetForPatching.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            SettledBet settledBet = new SettledBet(endRoundSettledBetForPatching);
            settledBet.setResultType(endRoundSettledBetForPatching.getGaResultType());

            processEndRoundLog.setRawBody(endRoundSettledBetForPatching.getRawData());
            processEndRoundLog.setRoundId(settledBet.getRoundId());
            processEndRoundLog.setVendorBetId(settledBet.getVendorBetId());

            httpRequestLog.setBetStart(System.currentTimeMillis());

            AgentPlayer agentPlayer = agentPlayerService.get(endRoundSettledBetForPatching.getAgentPlayerId());
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(endRoundSettledBetForPatching.getVendorPlayerId(), null);

            //get is bet = sidebet
            vendorService.verifyIsPreProcessingVendorGame(endRoundSettledBetForPatching.getVendorGameId());

            //get vendorCurrencyRate for the vendor
            GameSession gameSession = new GameSession(endRoundSettledBetForPatching);
            VendorCurrency vendorCurrency = vendorService.getCurrencyConversionRate(gameSession, newTraceId);

            //update settledBet info
            settledBetService.save(settledBet, settledBet.getRawData());

            //prepare insert new betHistory data
            BetHistory betHistory = new BetHistory(settledBet);
            if (!vendorService.getBetPreprocess().getIsPreProcessBet()) {
                // process bet as normal bet and send to kafka topic_bet_history topic
                kafkaService.produceBetHistory(betHistory, gameSession.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());
                // process bet as normal bet and send to kafka topic_warehouse_bet_history topic
                // kafkaService.produceWarehouseBetHistory
                //         (betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
                kafkaService.produceBetHistoryV3(betHistory, gameSession.getProductCode(), gameSession.getProductId(), gameSession.getProductGameId(),
                        gameSession.getAgentPlayerUsername(), gameSession.getVendorPlayerUsername());
            } else {
                // process bet as preprocessing bet and send to kafka topic_bet_history_preprocessing topic
                kafkaService.producePreprocessingBetHistory(betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
            }

            //prepare delete unsettledBet
            UnsettledBet unsettledBet = new UnsettledBet(settledBet);
            unsettledBetService.delete(unsettledBet);

            //prepare and send endRound to operator
            if (endRoundSettledBetForPatching.getSendToOperator() == 1) {
                this.notifyEndRoundProcess(newTraceId, agentPlayer, vendorPlayer, gameSession, settledBet, vendorCurrency.getFromVendorRate(), vendorCurrency.getToVendorRate(), endRoundSettledBetForPatching, settledBet, httpRequestLog);
            } else {
                GeneralVo vo = new GeneralVo();
                vo.setResponseCode(ResponseCode.SUCCESS);

                httpRequestLog.setId(newTraceId);
                httpRequestLog.setRoundId(settledBet.getRoundId());
                httpRequestLog.setRequestBody(endRoundSettledBetForPatching.getRawData());
                httpRequestLog.setAgentId(settledBet.getAgentId());
                httpRequestLog.setVendorBetId(settledBet.getVendorBetId());
                httpRequestLog.setVendorUsername(vendorPlayer.getUsername());
                httpRequestLog.setOperatorUsername(agentPlayer.getUsername());
                httpRequestLog.setVendorId(settledBet.getVendorId());
                httpRequestLog.setRequestType(WalletBetResultAction.class.getSimpleName());
                httpRequestLog.setGameToken(settledBet.getGameSessionToken());
                httpRequestLog.setBetEnd(System.currentTimeMillis());
                httpRequestLog.setBetTimeTaken(httpRequestLog.getBetEnd() - httpRequestLog.getBetStart());
                httpRequestLog.setOperatorStart(0L);
                httpRequestLog.setOperatorEnd(0L);
                httpService.end(httpRequestLog, vo);
            }

        } catch (GameNotSupportedException e) {
            exception = e;

        } catch (VendorCurrencyNotSupportException e) {
            exception = e;

        } catch (Exception e) {
            exception = e;

        } finally {
            if (exception != null) {
                //prepare and save processEndRoundLog if exception not null;
                processEndRoundLog.setEndTime(System.currentTimeMillis());
                RequestService.processEndRoundLogPatching(processEndRoundLog, exception, endRoundSettledBetForPatching);
            }
        }
    }

    private void notifyEndRoundProcess(String traceId, AgentPlayer agentPlayer, VendorPlayer vendorPlayer, GameSession gameSession, BetInformation betInformation, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate, EndRoundSettledBet endRoundSettledBet, SettledBet settledBet, HttpRequestLog httpRequestLog) {
        THREAD_POOL.submit(() -> {
            Exception exception = null;
            GeneralVo vo = new GeneralVo();
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);

            try {
                httpRequestLog.setId(traceId);
                httpRequestLog.setRoundId(settledBet.getRoundId());
                httpRequestLog.setRequestBody(endRoundSettledBet.getRawData());
                httpRequestLog.setAgentId(settledBet.getAgentId());
                httpRequestLog.setVendorBetId(settledBet.getVendorBetId());
                httpRequestLog.setVendorUsername(vendorPlayer.getUsername());
                httpRequestLog.setOperatorUsername(agentPlayer.getUsername());
                httpRequestLog.setVendorId(settledBet.getVendorId());
                httpRequestLog.setRequestType(WalletBetResultAction.class.getSimpleName());
                httpRequestLog.setGameToken(settledBet.getGameSessionToken());
                walletBetResultAction.callProcessEndRound(traceId, agentPlayer.getAgentId(), gameSession, betInformation, ResultType.END, fromVendorConversionRate, toVendorConversionRate, httpRequestLog);
                vo.setResponseCode(ResponseCode.SUCCESS);

            } catch (InvalidAgentApiCredentialException e) {
                httpService.logError(httpRequestLog, e);
                exception = e;

            } catch (Exception e) {
                httpService.logError(httpRequestLog, e);
                exception = e;

            } finally {
                httpRequestLog.setBetEnd(System.currentTimeMillis());
                httpRequestLog.setBetTimeTaken(httpRequestLog.getBetEnd() - httpRequestLog.getBetStart());
                httpService.end(httpRequestLog, vo);

            }
        });
    }

    @KafkaListener(topics = KafkaConstant.TOPIC_RAW_SETTLED_BET, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeRawSettledBet(String message) {
        String traceId = UUID.randomUUID().toString();
        HttpRequestLog httpRequestLog = httpService.startInternalConsumerForRawSettledBet();
        GeneralVo vo = new GeneralVo();

        try {
            SportRawSettledBet sportRawSettledBet = new Gson().fromJson(message, SportRawSettledBet.class);
            BetEvent responseVo = null;

            //to handle saba send results in bulk consists of refund bet.
            if (sportRawSettledBet.getBetStatus().equals(BetStatus.REFUNDED)) {
                CancelBetDto cancelBetDto = new CancelBetDto();
                cancelBetDto.setRefId(sportRawSettledBet.getRoundId());
                cancelBetDto.setOperationId(sportRawSettledBet.getExternalTransactionId());
                cancelBetDto.setUserId(sportRawSettledBet.getVendorPlayerUsername());
                responseVo = sportWalletService.refund(traceId, cancelBetDto, httpRequestLog);
                responseVo.setLastBalance(responseVo == null ? BigDecimal.ZERO : responseVo.getLastBalance());

            } else {
                responseVo = sportWalletService.settle(traceId, sportRawSettledBet, httpRequestLog);
            }

            vo.setBalance(responseVo.getLastBalance());
            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            e.printStackTrace();

        } finally {
            httpService.end(httpRequestLog, vo);

        }
    }
}