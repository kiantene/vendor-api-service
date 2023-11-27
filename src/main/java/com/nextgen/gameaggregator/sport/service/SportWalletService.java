package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.sport.bet.SportBetAction;
import com.nextgen.gameaggregator.operator.sport.settle.SportSettleAction;
import com.nextgen.gameaggregator.operator.sport.settle.SportWalletSettleAction;
import com.nextgen.gameaggregator.operator.sport.updatebet.SportUpdateBetAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetAction;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultAction;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.LoggingService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.sport.entity.SportBetResultData;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetMariaDB;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class SportWalletService {

    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private LoggingService loggingService;
    @Autowired
    private SportBetAction sportBetAction;
    @Autowired
    private SportSettleAction sportSettleAction;
    @Autowired
    private SportUnsettledBetService sportUnsettledBetService;
    @Autowired
    private SportUpdateBetAction sportUpdateBetAction;
    @Autowired
    private SportWalletSettleAction sportWalletSettleAction;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletBalanceAction walletBalanceAction;
    @Autowired
    private WalletBetAction walletBetAction;
    @Autowired
    private WalletBetResultAction walletBetResultAction;

    public BetEvent placeBet(String traceId, GameSession gameSession, SportBetResultData sportBetResultData, String rawData, HttpRequestLog httpRequestLog) throws VendorCurrencyNotSupportException, InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException {

        if (httpRequestLog != null) {
            httpRequestLog.setRequestType(WalletBetAction.class.getSimpleName());
            httpRequestLog.setOperatorUsername(gameSession.getAgentPlayerUsername());
            httpRequestLog.setVendorId(gameSession.getVendorId());
            httpRequestLog.setVendorBetId(sportBetResultData.getVendorBetId());
            httpRequestLog.setRoundId(sportBetResultData.getRoundId());
            httpRequestLog.setGameToken(gameSession.getToken());
            httpRequestLog.setBetStart(System.currentTimeMillis());
            httpRequestLog.setVendorUsername(gameSession.getVendorPlayerUsername());
            httpRequestLog.setVendorGameCode(gameSession.getVendorGameCode());
        }

        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = new SportUnsettledBetCouchbase(gameSession, rawData, sportBetResultData, traceId, ResultType.BET.code);
        BetEvent betEvent = null;

        try {
            WalletBalanceVo balanceVo = sportBetAction.call(traceId, gameSession, sportUnsettledBetCouchbase, httpRequestLog);
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balanceVo.getData().getBalance());
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            betEvent = new BetEvent(sportUnsettledBetCouchbase, null);

        } catch (Exception e) {
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            throw new InvalidOperatorResponseException();

        }

        if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

        return betEvent;
    }

    public BetEvent confirmBet(String traceId, GameSession gameSession, SportBetResultData sportBetResultData, String rawData, HttpRequestLog httpRequestLog) throws BetNotFoundException, InvalidOperatorResponseException, InsufficientBalanceException {

        if (httpRequestLog != null) {
            httpRequestLog.setRequestType(WalletBetAction.class.getSimpleName());
            httpRequestLog.setOperatorUsername(gameSession.getAgentPlayerUsername());
            httpRequestLog.setVendorId(gameSession.getVendorId());
            httpRequestLog.setVendorBetId(sportBetResultData.getVendorBetId());
            httpRequestLog.setRoundId(sportBetResultData.getRoundId());
            httpRequestLog.setGameToken(gameSession.getToken());
            httpRequestLog.setBetStart(System.currentTimeMillis());
            httpRequestLog.setVendorUsername(gameSession.getVendorPlayerUsername());
            httpRequestLog.setVendorGameCode(gameSession.getVendorGameCode());
        }

        loggingService.logStart();

        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportUnsettledBetService.couchbaseGetByExternalTransactionId(gameSession.getVendorPlayerUsername(), sportBetResultData.getExternalTransactionId());
        sportUnsettledBetCouchbase.setNewBetAmount(sportBetResultData.getNewBetAmount());
        sportUnsettledBetCouchbase.setVendorBetId(sportBetResultData.getVendorBetId());
        sportUnsettledBetCouchbase.setExternalTransactionId(sportBetResultData.getExternalTransactionId());
        SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);

        loggingService.logProcessTime("processBet ｜ unsettledBetService.idempotentCheck", traceId);

        BetEvent betEvent = null;
        try {
            // record operator processing time
            WalletBalanceVo balanceVo = sportUpdateBetAction.call(traceId, gameSession, sportUnsettledBetCouchbase, httpRequestLog);
            BigDecimal balance = balanceVo.getData().getBalance();
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balance);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);

            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB);
            betEvent = new BetEvent(sportUnsettledBetCouchbase, balance);

        } catch (InvalidOperatorResponseException e) {

            // record status code from operator if they return an error
            Integer operatorStatus = e.getOperatorStatus();
            sportUnsettledBetCouchbase.setOperatorStatus(operatorStatus);

            if (operatorStatus.equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                loggingService.logStart();
                loggingService.logProcessTime("Sport ConfirmBet ｜ when invalidOperatorResponseException.SC_INSUFFICIENT_FUNDS", traceId);
                throw new InsufficientBalanceException();

            } else {
                loggingService.logStart();
                loggingService.logProcessTime("Sport ConfirmBet ｜ when invalidOperatorResponseException", traceId);
                throw e;

            }

        } catch (Exception e) {
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            throw new InvalidOperatorResponseException();

        }

        if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

        return betEvent;
    }

    public void settle2(SportSettledBet sportSettledBet) throws BetNotFoundException, InvalidAgentApiCredentialException, RecordNotFoundException {
        String traceId = UUID.randomUUID().toString();
        SportUnsettledBetCouchbase unsettledBetCouchbase = sportUnsettledBetService.couchbaseGetByExternalTransactionId(sportSettledBet.getVendorPlayerUsername(), sportSettledBet.getExternalTransactionId());
        SportUnsettledBetMariaDB unsettledBetMariaDB = new SportUnsettledBetMariaDB(unsettledBetCouchbase);
        unsettledBetMariaDB.setId(unsettledBetCouchbase.getBetId());
        sportWalletSettleAction.call(traceId, unsettledBetMariaDB, sportSettledBet);
        BetHistory betHistory = sportSettledBet.toBetHistory(unsettledBetMariaDB);
        kafkaService.produceBetHistory(betHistory, null, BigDecimal.ONE);
    }

    public void settle(SportBetResultData sportBetResultData, HttpRequestLog httpRequestLog) throws BetNotFoundException, InvalidAgentApiCredentialException, RecordNotFoundException, InvalidOperatorResponseException {

        String traceId = UUID.randomUUID().toString();
        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportUnsettledBetService.couchbaseGetByExternalTransactionId(sportBetResultData.getVendorPlayerUsername(), sportBetResultData.getExternalTransactionId());
        sportUnsettledBetCouchbase.setWinAmount(sportBetResultData.getWinAmount());

        BigDecimal newBetAmount = sportUnsettledBetCouchbase.getNewBetAmount() != null ? sportUnsettledBetCouchbase.getNewBetAmount() : sportUnsettledBetCouchbase.getBetAmount();
        sportUnsettledBetCouchbase.setWinLoss(sportBetResultData.getWinAmount().subtract(newBetAmount));
        sportUnsettledBetCouchbase.setEffectiveTurnover(sportUnsettledBetCouchbase.getNewBetAmount());

        try {
            sportSettleAction.call(traceId, sportUnsettledBetCouchbase.getGameSession(), sportUnsettledBetCouchbase, httpRequestLog);
            sportUnsettledBetService.delete(sportUnsettledBetCouchbase);

        } catch (Exception e) {
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            throw new InvalidOperatorResponseException();

        }

        BetHistory betHistory = sportUnsettledBetCouchbase.toBetHistory();
        kafkaService.produceBetHistory(betHistory, null, BigDecimal.ONE);
    }

    @Async
    public void batchSettle(List<SportBetResultData> sportBetResultDataList, String rawData) throws InvalidAgentApiCredentialException, RecordNotFoundException, BetNotFoundException, InvalidOperatorResponseException {
        for (SportBetResultData sportBetResultData : sportBetResultDataList) {
            SportSettledBet sportSettledBet = new SportSettledBet(sportBetResultData, rawData);
//            kafkaService.produceSettledBet(sportSettledBet);
            this.settle(sportBetResultData, null);
        }
    }
}