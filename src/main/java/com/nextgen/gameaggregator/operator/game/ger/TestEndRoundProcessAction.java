package com.nextgen.gameaggregator.operator.game.ger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.eventing.events.ResultBetOperatorFailEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.game.url.GameUrlDto;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultAction;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.pragmaticplay.api.bet.BetDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = "game/")
@Slf4j
public class TestEndRoundProcessAction {

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
    private HttpService httpService;

    @PostMapping(path = "ger")
    public void consumeEndRoundProcess(HttpServletRequest request) throws InterruptedException, InvalidRequestException, JsonProcessingException {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String body = httpRequestLog.getRequestBody();
        EndRoundSettledBet endRoundSettledBet = HttpService.convertJsonToDto(body, EndRoundSettledBet.class);

        ProcessEndRoundLog processEndRoundLog = new ProcessEndRoundLog();
        processEndRoundLog.setStartTime(System.currentTimeMillis());
        Long currentTime = System.currentTimeMillis();
        String vendorBetId = endRoundSettledBet.getVendorBetId();
        String roundId = endRoundSettledBet.getRoundId();
        Exception exception = null;
        String newTraceId = UUID.randomUUID().toString();
        boolean isOperatorFailed = false;
        processEndRoundLog.setOperatorProcessStartTime(0l);
        processEndRoundLog.setOperatorProcessEndTime(0l);
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;
        Integer updatedOperatorStatus = operatorStatusProcessing;

        try {
            //0. check bet exists
            this.doCheckExistsForSettledBet(endRoundSettledBet);

            //0. check is it time to process, if not then push back to kafkaServices
            if(endRoundSettledBet.getEndRoundProcessTime() > currentTime){
                this.doSendBackToProcessEndRoundKafka(endRoundSettledBet);
            }
            else{
                //1. check end round process counter and get internal transaction id
                String internalTransactionId = this.doCheckEndRoundProcessCounterAndGetInternalTransactionId(endRoundSettledBet, newTraceId);
                endRoundSettledBet.setInternalTransactionId(internalTransactionId);
                endRoundSettledBet.setOperatorStatus(operatorStatusProcessing);

                //2. get agentPlayerUsername, currencyCode and gameCode into gameSession for walletBetResultAction.call
                GameSession gameSession = new GameSession(endRoundSettledBet);

                //3. convert endRoundSettledBet back to settledBet
                SettledBet settledBet = new SettledBet(endRoundSettledBet);

                //4. send the bet data with resultType end to operator
                processEndRoundLog.setOperatorProcessStartTime(System.currentTimeMillis());
                walletBetResultAction.call(newTraceId, endRoundSettledBet.getAgentId(), gameSession, settledBet, ResultType.END, null);
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
                kafkaService.produceBetHistory(betHistory, settledBet);

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
            //SC_EXCEEDED_NUMBER_OF_RETRIES(31, "Exceeded number of retries."),
            updatedOperatorStatus = 31;

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            //do nothing and save the processEndRoundLog only, because the data has been successfully processed

        }catch (Exception e) {
            exception = e;
            isOperatorFailed = true;

        } finally {
            if (isOperatorFailed) {
                Integer endRoundCounter = endRoundSettledBet.getProcessEndRoundCounter() + 1;
                Long nextProcessTime = (endRoundCounter.longValue() * 10000) + endRoundSettledBet.getEndRoundProcessTime();

                endRoundSettledBet.setProcessEndRoundCounter(endRoundCounter);
                endRoundSettledBet.setEndRoundProcessTime(nextProcessTime);
                endRoundSettledBet.setOperatorStatus(updatedOperatorStatus);

                this.doSendBackToProcessEndRoundKafka(endRoundSettledBet);
                SettledBet updatedSettledBet = new SettledBet(endRoundSettledBet);
                settledBetService.save(updatedSettledBet, updatedSettledBet.getRawData());
            }

            //9. prepare to log process end round details
            processEndRoundLog.setTraceId(newTraceId);
            processEndRoundLog.setRoundId(roundId);
            processEndRoundLog.setVendorBetId(vendorBetId);
            processEndRoundLog.setEndTime(System.currentTimeMillis());
            processEndRoundLog.setRawBody(body);
            requestService.processEndRoundLog(processEndRoundLog, exception, endRoundSettledBet);
        }

    }

    public void doSendBackToProcessEndRoundKafka(EndRoundSettledBet endRoundSettledBet) throws InterruptedException {
        Thread.sleep(10000);
        kafkaService.produceEndRoundSettleBet(endRoundSettledBet);
    }

    public void doCheckExistsForSettledBet(EndRoundSettledBet endRoundSettledBet) throws BetResultIdempotentViolationException {
        try{
            SettledBet idempotentSettleBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(endRoundSettledBet.getVendorBetId(), endRoundSettledBet.getRoundId(), endRoundSettledBet.getVendorId(), endRoundSettledBet.getVendorPlayerId());

            if(idempotentSettleBet.getOperatorStatus() == 1){
                //if it is success processed, then throw idempotent and stop to process again
                throw new BetResultIdempotentViolationException();
            }
            else{
                // else if it is not success, will send the end to operator
            }

        } catch (BetNotFoundException betNotFoundException){
            //betNotFound is correct behavior, so will continue to process the rest of the process
        }
    }

    public String doCheckEndRoundProcessCounterAndGetInternalTransactionId(EndRoundSettledBet endRoundSettledBet, String newTraceId) throws ExceedThresholdCounterException {

        String internalTransactionId = newTraceId;
        Integer firstTimeProcess = 0;
        Integer exceedThresholdCounter = 5;

        if(endRoundSettledBet.getProcessEndRoundCounter() >= exceedThresholdCounter){
            //if more than 5 times, throw ExceedThresholdCounterException and logged down separately
            throw new ExceedThresholdCounterException();

        } else if (endRoundSettledBet.getProcessEndRoundCounter() != firstTimeProcess){
            //if not first time process, will reuse the internalTransactionId
            internalTransactionId = endRoundSettledBet.getInternalTransactionId();

        }

        return internalTransactionId;
    }
}
