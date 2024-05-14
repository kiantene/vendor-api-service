package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultAction;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class KafkaConsumerService {

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

    @KafkaListener(topics = KafkaConstant.TOPIC_END_ROUND_PROCESS, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeEndRoundProcess(String message) {

        //prepare endRoundProcess Log
        Exception exception = null;
        String newTraceId = UUID.randomUUID().toString();
        ProcessEndRoundLog processEndRoundLog = new ProcessEndRoundLog();
        processEndRoundLog.setStartTime(System.currentTimeMillis());
        processEndRoundLog.setOperatorProcessStartTime(System.currentTimeMillis());
        processEndRoundLog.setOperatorProcessEndTime(System.currentTimeMillis());

        //TEST PRINT TO QA
        log.info("PP TOPIC_END_ROUND_PROCESS = " + KafkaConstant.TOPIC_END_ROUND_PROCESS);
        log.info("PP TOPIC_END_ROUND_PROCESS GROUP_ID = " + KafkaConstant.GROUP_ID);

        //prepare endRound and settleBet info
        EndRoundSettledBet endRoundSettledBet = new Gson().fromJson(message, EndRoundSettledBet.class);
        endRoundSettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
        SettledBet settledBet = new SettledBet(endRoundSettledBet);
        settledBet.setResultType(endRoundSettledBet.getGaResultType());

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
            } else {
                // process bet as preprocessing bet and send to kafka topic_bet_history_preprocessing topic
                kafkaService.producePreprocessingBetHistory(betHistory, settledBet, vendorCurrency.getFromVendorRate());
            }

            //prepare delete unsettledBet
            UnsettledBet unsettledBet = new UnsettledBet(settledBet);
            unsettledBetService.delete(unsettledBet);

            //prepare and send endRound to operator
            processEndRoundLog.setOperatorProcessStartTime(System.currentTimeMillis());
            walletBetResultAction.callProcessEndRound(newTraceId, endRoundSettledBet.getAgentId(), gameSession, settledBet, ResultType.END, null, vendorCurrency.getFromVendorRate(), vendorCurrency.getToVendorRate());
            processEndRoundLog.setOperatorProcessEndTime(System.currentTimeMillis());

        } catch (GameNotSupportedException e) {
            exception = e;

        } catch (InvalidAgentApiCredentialException e) {
            exception = e;

        } catch (VendorCurrencyNotSupportException e) {
            exception = e;

        } catch (Exception e) {
            exception = e;

        } finally {
            //prepare and save processEndRoundLog
            processEndRoundLog.setTraceId(newTraceId);
            processEndRoundLog.setRoundId(settledBet.getRoundId());
            processEndRoundLog.setVendorBetId(settledBet.getVendorBetId());
            processEndRoundLog.setEndTime(System.currentTimeMillis());
            processEndRoundLog.setRawBody(endRoundSettledBet.getRawData());
            requestService.processEndRoundLog(processEndRoundLog, exception, endRoundSettledBet);

        }

    }

    public void doSendBackToProcessEndRoundKafka(EndRoundSettledBet endRoundSettledBet, String traceId) throws InterruptedException {
        loggingService.logStart();
        Thread.sleep(10000);
        loggingService.logProcessTime("doSendBackToProcessEndRoundKafka", traceId);
        kafkaService.produceEndRoundSettleBet(endRoundSettledBet);
    }

    public void doCheckExistsForSettledBet(EndRoundSettledBet endRoundSettledBet) throws BetResultIdempotentViolationException {
        try {
            SettledBet idempotentSettleBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(endRoundSettledBet.getVendorBetId(), endRoundSettledBet.getRoundId(), endRoundSettledBet.getVendorId(), endRoundSettledBet.getVendorPlayerId());

            if (idempotentSettleBet.getOperatorStatus() == 1) {
                //if it is success processed, then throw idempotent and stop to process again
                throw new BetResultIdempotentViolationException();
            } else {
                // else if it is not success, will send the end to operator
            }

        } catch (BetNotFoundException betNotFoundException) {
            //betNotFound is correct behavior, so will continue to process the rest of the process
        }
    }

    public Boolean doCheckExceedThresholdCounter(EndRoundSettledBet endRoundSettledBet, String newTraceId) {
        //5 = 2.5 minutes
        //10 = 9.17 minutes
        //15 = 20 minutes
        Integer exceedThresholdCounter = 10;
        if (endRoundSettledBet.getProcessEndRoundCounter() >= exceedThresholdCounter) {
            //if retry more than 10 times, return true and not send to operator
            return true;
        }
        return false;
    }

    @KafkaListener(topics = KafkaConstant.TOPIC_RAW_SETTLED_BET, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeRawSettledBet(String message) {
        String traceId = UUID.randomUUID().toString();
        HttpRequestLog httpRequestLog = httpService.startInternalConsumerForRawSettledBet();
        GeneralVo vo = new GeneralVo();

        try {
            SportRawSettledBet sportRawSettledBet = new Gson().fromJson(message, SportRawSettledBet.class);
            BetEvent responseVo = sportWalletService.settle(traceId, sportRawSettledBet, httpRequestLog);
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
