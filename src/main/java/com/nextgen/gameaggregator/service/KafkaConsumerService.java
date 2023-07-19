package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.eventing.events.ResultBetOperatorFailEvent;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultAction;
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

    @KafkaListener(topics = KafkaConstant.TOPIC_END_ROUND_PROCESS, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeEndRoundProcess(String message) {

        //0. set default value and start counting the entire process start time
        ProcessEndRoundLog processEndRoundLog = new ProcessEndRoundLog();
        processEndRoundLog.setStartTime(System.currentTimeMillis());

        String vendorBetId = null;
        String roundId = null;
        Exception exception = null;
        ResultBetOperatorFailEvent resultBetOperatorFailEvent = null;
        String newTraceId = UUID.randomUUID().toString();
        Integer status = 1;

        try {
            //1. convert json string back to endRoundSettledBet
            EndRoundSettledBet endRoundSettledBet = new Gson().fromJson(message, EndRoundSettledBet.class);

            //2. get agentPlayerUsername, currencyCode and gameCode into gameSession for walletBetResultAction.call
            GameSession gameSession = new GameSession(endRoundSettledBet);

            //3. convert endRoundSettledBet back to settledBet
            SettledBet settledBet = new SettledBet(endRoundSettledBet);
            vendorBetId = settledBet.getVendorBetId();
            roundId = settledBet.getRoundId();

            //4. send the bet data with resultType end to operator
            processEndRoundLog.setOperatorProcessStartTime(System.currentTimeMillis());
            walletBetResultAction.call(newTraceId, endRoundSettledBet.getAgentId(), gameSession, settledBet, ResultType.END, null);
            processEndRoundLog.setOperatorProcessEndTime(System.currentTimeMillis());

            //5. set the resultType as endRoundSettledBet.getGaResultType() which calculated in processBetResult
            settledBet.setResultType(endRoundSettledBet.getGaResultType());

            //6. create temp settleBet on couchbase
            settledBetService.save(settledBet, settledBet.getRawData());

            BetHistory betHistory = new BetHistory(settledBet);

            //7. log success betHistory before send to kafka
            log.info(new Gson().toJson(betHistory));

            //8. send to process bet history kafka topic
            kafkaService.produceBetHistory(betHistory, settledBet);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            exception = invalidAgentApiCredentialException;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            exception = invalidOperatorResponseException;

        } catch (Exception e) {
            exception = e;

        } finally {
            boolean isOperatorFailed = resultBetOperatorFailEvent != null;
            if (isOperatorFailed) {
                status = 2;
                // TODO: if operator failed :
                // TODO : option 1, send back to TOPIC_END_ROUND_PROCESS for it to process again
                // TODO : option 2, send to a new TOPIC to handle as exceptional case
            }

            //9. prepare to log process end round details
            processEndRoundLog.setTraceId(newTraceId);
            processEndRoundLog.setRoundId(roundId);
            processEndRoundLog.setVendorBetId(vendorBetId);
            processEndRoundLog.setEndTime(System.currentTimeMillis());
            processEndRoundLog.setRawBody(message);
            processEndRoundLog.setStatus(status);
            requestService.processEndRoundLog(processEndRoundLog, exception);
        }

    }
}
