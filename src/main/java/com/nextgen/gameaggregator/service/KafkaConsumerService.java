package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.ExceedThresholdCounterException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultAction;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
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

    @KafkaListener(topics = KafkaConstant.TOPIC_END_ROUND_PROCESS, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeEndRoundProcess(String message) throws InterruptedException {

        //0. set default value and start counting the entire process start time
        ProcessEndRoundLog processEndRoundLog = new ProcessEndRoundLog();
        processEndRoundLog.setStartTime(System.currentTimeMillis());

        Long currentTime = System.currentTimeMillis();
        Exception exception = null;
        String newTraceId = UUID.randomUUID().toString();
        boolean isOperatorFailed = false;

        EndRoundSettledBet endRoundSettledBet = new Gson().fromJson(message, EndRoundSettledBet.class);
        String vendorBetId = endRoundSettledBet.getVendorBetId();
        String roundId = endRoundSettledBet.getRoundId();
        processEndRoundLog.setOperatorProcessStartTime(0L);
        processEndRoundLog.setOperatorProcessEndTime(0L);
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;
        Integer operatorStatusExceededNumOfRetries = ResponseCodes.Status.SC_EXCEEDED_NUMBER_OF_RETRIES.code;
        Integer updatedOperatorStatus = operatorStatusProcessing;

        try {
            //0. check bet exists
            this.doCheckExistsForSettledBet(endRoundSettledBet);

            //0. check is it time to process, if not then push back to kafkaServices
            if (endRoundSettledBet.getEndRoundProcessTime() > currentTime) {
                this.doSendBackToProcessEndRoundKafka(endRoundSettledBet, newTraceId);
            } else {
                //1. check end round process counter and get internal transaction id
                this.doCheckExceedThresholdCounter(endRoundSettledBet, newTraceId);
                endRoundSettledBet.setOperatorStatus(operatorStatusProcessing);

                //2. get agentPlayerUsername, currencyCode and gameCode into gameSession for walletBetResultAction.call
                GameSession gameSession = new GameSession(endRoundSettledBet);

                //3. convert endRoundSettledBet back to settledBet
                SettledBet settledBet = new SettledBet(endRoundSettledBet);

                VendorCurrency vendorCurrency = vendorService.getCurrencyConversionRate(gameSession, newTraceId);

                //4. send the bet data with resultType end to operator
                processEndRoundLog.setOperatorProcessStartTime(System.currentTimeMillis());
                walletBetResultAction.call(newTraceId, endRoundSettledBet.getAgentId(), gameSession, settledBet, ResultType.END, null, vendorCurrency.getFromVendorRate(), vendorCurrency.getToVendorRate());
                processEndRoundLog.setOperatorProcessEndTime(System.currentTimeMillis());

                //5. set the resultType as endRoundSettledBet.getGaResultType() which calculated in processBetResult
                settledBet.setResultType(endRoundSettledBet.getGaResultType());

                //6. create temp settleBet on couchbase
                endRoundSettledBet.setOperatorStatus(operatorStatusSuccess);
                settledBet.setOperatorStatus(operatorStatusSuccess);
                settledBetService.save(settledBet, settledBet.getRawData());

                //7. log success betHistory before send to kafka
                BetHistory betHistory = new BetHistory(settledBet);
                log.info(new Gson().toJson(betHistory));

                //8. send to process bet history kafka topic
                kafkaService.produceBetHistory(betHistory, settledBet, vendorCurrency.getFromVendorRate());

                //delete unsettled bet
                UnsettledBet unsettledBet = new UnsettledBet(settledBet);
                unsettledBetService.delete(unsettledBet);
            }

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            exception = invalidOperatorResponseException;
            updatedOperatorStatus = invalidOperatorResponseException.getOperatorStatus();
            isOperatorFailed = true;

        } catch (ExceedThresholdCounterException exceedThresholdCounterException) {
            //will no longer send back to process end round kafka, will log down for now
            exception = exceedThresholdCounterException;
            updatedOperatorStatus = operatorStatusExceededNumOfRetries;

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            //do nothing and save the processEndRoundLog only, because the data has been successfully processed

        } catch (Exception e) {
            //if any other exception, will be sent back to kafka and process again with operatorStatusProcessing status
            exception = e;
            isOperatorFailed = true;

        } finally {
            if (isOperatorFailed) {
                //every fail, increase 1 endRoundCounter and increase nextProcessTime by 10 secs
                Integer endRoundCounter = endRoundSettledBet.getProcessEndRoundCounter() + 1;
                Long nextProcessTime = (endRoundCounter.longValue() * 10000) + endRoundSettledBet.getEndRoundProcessTime();

                endRoundSettledBet.setProcessEndRoundCounter(endRoundCounter);
                endRoundSettledBet.setEndRoundProcessTime(nextProcessTime);
                endRoundSettledBet.setOperatorStatus(updatedOperatorStatus);

                //send back to kafka and update the settle bet operatorStatus for check exists
                this.doSendBackToProcessEndRoundKafka(endRoundSettledBet, newTraceId);
                SettledBet updatedSettledBet = new SettledBet(endRoundSettledBet);
                settledBetService.save(updatedSettledBet, updatedSettledBet.getRawData());
            }

            //9. prepare to log process end round details
            processEndRoundLog.setTraceId(newTraceId);
            processEndRoundLog.setRoundId(roundId);
            processEndRoundLog.setVendorBetId(vendorBetId);
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

    public void doCheckExceedThresholdCounter(EndRoundSettledBet endRoundSettledBet, String newTraceId) throws ExceedThresholdCounterException {

        //5 = 2.5 minutes
        //10 = 9.17 minutes
        //15 = 20 minutes
        Integer exceedThresholdCounter = 15;

        if (endRoundSettledBet.getProcessEndRoundCounter() >= exceedThresholdCounter) {
            //if more than 5 times, throw ExceedThresholdCounterException and logged down separately
            throw new ExceedThresholdCounterException();

        }
    }

    @KafkaListener(topics = KafkaConstant.TOPIC_RAW_SETTLED_BET, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeRawSettledBet(String message) {
        String traceId = UUID.randomUUID().toString();

        try {
            SportRawSettledBet sportRawSettledBet = new Gson().fromJson(message, SportRawSettledBet.class);
            sportWalletService.settle(traceId, sportRawSettledBet, null);

        } catch (Exception e) {

        }
    }
}
