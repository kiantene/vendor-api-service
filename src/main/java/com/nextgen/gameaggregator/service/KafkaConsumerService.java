package com.nextgen.gameaggregator.service;

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

    @KafkaListener(topics = KafkaConstant.TOPIC_END_ROUND_PROCESS, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeEndRoundProcess(String message) {

        //prepare endRoundProcess Log
        Exception exception = null;
        String newTraceId = UUID.randomUUID().toString();
        ProcessEndRoundLog processEndRoundLog = new ProcessEndRoundLog();
        processEndRoundLog.setStartTime(System.currentTimeMillis());

        //prepare endRound and settleBet info
        EndRoundSettledBet endRoundSettledBet = new Gson().fromJson(message, EndRoundSettledBet.class);
        endRoundSettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
        SettledBet settledBet = new SettledBet(endRoundSettledBet);
        settledBet.setResultType(endRoundSettledBet.getGaResultType());
        processEndRoundLog.setTraceId(newTraceId);
        processEndRoundLog.setRoundId(settledBet.getRoundId());
        processEndRoundLog.setVendorBetId(settledBet.getVendorBetId());
        processEndRoundLog.setRawBody(endRoundSettledBet.getRawData());

        try {
            //get is bet = sidebet
            vendorService.verifyIsPreProcessingVendorGame(endRoundSettledBet.getVendorGameId());

            AgentPlayer agentPlayer = agentPlayerService.getByAgentPlayerId(endRoundSettledBet.getAgentPlayerId(), null);
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(endRoundSettledBet.getVendorPlayerId(), null);

            //get vendorCurrencyRate for the vendor
            GameSession gameSession = new GameSession(endRoundSettledBet);
            VendorCurrency vendorCurrency = vendorService.getCurrencyConversionRate(gameSession, newTraceId);

            //update settledBet info
            settledBetService.save(settledBet, settledBet.getRawData());

            //prepare insert new betHistory data
            BetHistory betHistory = new BetHistory(settledBet);
            if (!vendorService.getBetPreprocess().getIsPreProcessBet()) {
                // process bet as normal bet and send to kafka topic_bet_history topic
                kafkaService.produceBetHistory(betHistory, settledBet, vendorCurrency.getFromVendorRate());
                // process bet as normal bet and send to kafka topic_warehouse_bet_history topic
                kafkaService.produceWarehouseBetHistory
                        (betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
            } else {
                // process bet as preprocessing bet and send to kafka topic_bet_history_preprocessing topic
                kafkaService.producePreprocessingBetHistory(betHistory, settledBet, vendorCurrency.getFromVendorRate());
            }

            //prepare delete unsettledBet
            UnsettledBet unsettledBet = new UnsettledBet(settledBet);
            unsettledBetService.delete(unsettledBet);

            //prepare and send endRound to operator
            this.notifyEndRoundProcess(newTraceId, endRoundSettledBet.getAgentId(), gameSession, settledBet, vendorCurrency.getFromVendorRate(), vendorCurrency.getToVendorRate(), processEndRoundLog, endRoundSettledBet, settledBet);

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

    @KafkaListener(topics = KafkaConstant.TOPIC_END_ROUND_PROCESS_V2, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeEndRoundProcessV2(String message) throws RecordNotFoundException, InvalidPlayerException {

        //prepare endRoundProcess Log
        Exception exception = null;
        String newTraceId = UUID.randomUUID().toString();
        ProcessEndRoundLog processEndRoundLog = new ProcessEndRoundLog();
        processEndRoundLog.setStartTime(System.currentTimeMillis());
        processEndRoundLog.setTraceId(newTraceId);

        //prepare endRound and settleBet info
        EndRoundSettledBet endRoundSettledBet = new Gson().fromJson(message, EndRoundSettledBet.class);
        endRoundSettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
        SettledBet settledBet = new SettledBet(endRoundSettledBet);
        settledBet.setResultType(endRoundSettledBet.getGaResultType());

        AgentPlayer agentPlayer = agentPlayerService.getByAgentPlayerId(endRoundSettledBet.getAgentPlayerId(), null);
        VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(endRoundSettledBet.getVendorPlayerId(), null);

        processEndRoundLog.setRawBody(endRoundSettledBet.getRawData());
        processEndRoundLog.setRoundId(settledBet.getRoundId());
        processEndRoundLog.setVendorBetId(settledBet.getVendorBetId());

        try {
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
                kafkaService.produceBetHistory(betHistory, settledBet, vendorCurrency.getFromVendorRate());
                // process bet as normal bet and send to kafka topic_warehouse_bet_history topic
                kafkaService.produceWarehouseBetHistory
                        (betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
            } else {
                // process bet as preprocessing bet and send to kafka topic_bet_history_preprocessing topic
                kafkaService.producePreprocessingBetHistory(betHistory, settledBet, vendorCurrency.getFromVendorRate());
            }

            //prepare delete unsettledBet
            UnsettledBet unsettledBet = new UnsettledBet(settledBet);
            unsettledBetService.delete(unsettledBet);

            //prepare and send endRound to operator
            this.notifyEndRoundProcess(newTraceId, endRoundSettledBet.getAgentId(), gameSession, settledBet, vendorCurrency.getFromVendorRate(), vendorCurrency.getToVendorRate(), processEndRoundLog, endRoundSettledBet, settledBet);

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

    private void notifyEndRoundProcess(String traceId, Integer agentId, GameSession gameSession, BetInformation betInformation, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate, ProcessEndRoundLog processEndRoundLog, EndRoundSettledBet endRoundSettledBet, SettledBet settledBet) {
        THREAD_POOL.submit(() -> {
            Exception exception = null;

            try {
                processEndRoundLog.setOperatorProcessStartTime(System.currentTimeMillis());
                processEndRoundLog.setTraceId(traceId);
                processEndRoundLog.setRoundId(settledBet.getRoundId());
                processEndRoundLog.setVendorBetId(settledBet.getVendorBetId());
                processEndRoundLog.setRawBody(endRoundSettledBet.getRawData());
                walletBetResultAction.callProcessEndRound(traceId, agentId, gameSession, betInformation, ResultType.END, fromVendorConversionRate, toVendorConversionRate);

            } catch (InvalidAgentApiCredentialException e) {
                exception = e;

            } catch (Exception e) {
                exception = e;

            } finally {
                //prepare and save processEndRoundLog
                processEndRoundLog.setOperatorProcessEndTime(System.currentTimeMillis());
                processEndRoundLog.setEndTime(System.currentTimeMillis());
                RequestService.processEndRoundLog(processEndRoundLog, exception, endRoundSettledBet);

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