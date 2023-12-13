package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.sport.bet.SportBetAction;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundAction;
import com.nextgen.gameaggregator.operator.sport.settle.SportSettleAction;
import com.nextgen.gameaggregator.operator.sport.settle.SportWalletSettleAction;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleAction;
import com.nextgen.gameaggregator.operator.sport.updatebet.SportUpdateBetAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetAction;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.LoggingService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.sport.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SportWalletService {

    @Autowired
    private BetHistoryRepository betHistoryRepository;
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
    private SportRefundAction sportRefundAction;
    @Autowired
    private SportUnsettleAction sportUnsettleAction;
    @Autowired
    private VendorPlayerService vendorPlayerService;

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

            SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB);

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

    public BetEvent settle(String traceId, SportBetResultData sportBetResultData, HttpRequestLog httpRequestLog) throws BetNotFoundException, InvalidAgentApiCredentialException, RecordNotFoundException, InvalidOperatorResponseException {

        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportUnsettledBetService.couchbaseGetByExternalTransactionId(sportBetResultData.getVendorPlayerUsername(), sportBetResultData.getExternalTransactionId());
        sportUnsettledBetCouchbase.setWinAmount(sportBetResultData.getWinAmount());

        BigDecimal newBetAmount = sportUnsettledBetCouchbase.getNewBetAmount() != null ? sportUnsettledBetCouchbase.getNewBetAmount() : sportUnsettledBetCouchbase.getBetAmount();
        sportUnsettledBetCouchbase.setWinLoss(sportBetResultData.getWinAmount().subtract(newBetAmount));
        sportUnsettledBetCouchbase.setEffectiveTurnover(sportUnsettledBetCouchbase.getNewBetAmount());
        sportUnsettledBetCouchbase.setResettleNum((sportUnsettledBetCouchbase.getResettleNum() != null && sportUnsettledBetCouchbase.getResettleNum() >= 0) ? sportUnsettledBetCouchbase.getResettleNum() + 1 : 0);

        BetEvent betEvent = null;

        try {
            WalletBalanceVo balanceVo = sportSettleAction.call(traceId, sportUnsettledBetCouchbase, sportUnsettledBetCouchbase, httpRequestLog);
            betEvent = new BetEvent(sportUnsettledBetCouchbase, null);
//            sportUnsettledBetService.delete(sportUnsettledBetCouchbase);

        } catch (Exception e) {
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            throw new InvalidOperatorResponseException();

        }

        Integer betStatus = BetStatus.SETTLED.code;
        BigDecimal winAmount = sportBetResultData.getWinAmount();
        BigDecimal betAmount = sportUnsettledBetCouchbase.getNewBetAmount() == null ? sportUnsettledBetCouchbase.getBetAmount() : sportUnsettledBetCouchbase.getNewBetAmount();
        int resultType = winAmount.compareTo(betAmount) > 0 ? BetResultType.WIN.code : BetResultType.LOSE.code;
        BetHistory betHistory = sportUnsettledBetCouchbase.toBetHistory(betStatus, resultType);
        kafkaService.produceBetHistory(betHistory, null, BigDecimal.ONE);

        return betEvent;
    }

    @Async
    public void batchSettle(List<SportBetResultData> sportBetResultDataList, String rawData) throws InvalidAgentApiCredentialException, RecordNotFoundException, BetNotFoundException, InvalidOperatorResponseException {
        for (SportBetResultData sportBetResultData : sportBetResultDataList) {
            SportSettledBet sportSettledBet = new SportSettledBet(sportBetResultData, rawData);
//            kafkaService.produceSettledBet(sportSettledBet);
            String traceId = UUID.randomUUID().toString();
            this.settle(traceId, sportBetResultData, null);
        }
    }

    public BetEvent refund(String traceId, SportRefundData sportRefundData, String rawData, HttpRequestLog httpRequestLog) throws VendorCurrencyNotSupportException,
        InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, BetNotFoundException {

        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportUnsettledBetService.couchbaseGetByExternalTransactionId(sportRefundData.getVendorPlayerUsername(), sportRefundData.getExternalTransactionId());
        BetEvent betEvent = null;
        Integer betStatus = BetStatus.REFUNDED.code;

        try {
            WalletBalanceVo balanceVo = sportRefundAction.call(traceId, sportUnsettledBetCouchbase, httpRequestLog);
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balanceVo.getData().getBalance());
            sportUnsettledBetCouchbase.setStatus(betStatus);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            betEvent = new BetEvent(sportUnsettledBetCouchbase, null);

        } catch (Exception e) {
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            throw new InvalidOperatorResponseException();

        }

        if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

        SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
        kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB);

        BetHistory betHistory = sportUnsettledBetCouchbase.toBetHistory(betStatus, BetResultType.BET.code);
        kafkaService.produceBetHistory(betHistory, null, BigDecimal.ONE);

        return betEvent;
    }

    public BetEvent unsettle(String traceId, SportUnsettleData sportUnsettleData, String rawData, HttpRequestLog httpRequestLog) throws VendorCurrencyNotSupportException,
            InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, BetNotFoundException, InvalidPlayerException {
        
        BetEvent betEvent = null;
        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(sportUnsettleData.getVendorPlayerUsername());
        BetHistory betHistory = betHistoryRepository.findByExternalTransactionIdAndVendorId(sportUnsettleData.getExternalTransactionId(), vendorPlayer.getVendorId());
        if (betHistory == null) throw new BetNotFoundException();

        try {
            String couchbaseId = sportUnsettleData.getVendorPlayerUsername() + '_' + sportUnsettleData.getExternalTransactionId();
            SportUnsettledBetCouchbase sportUnsettledBetCouchbase = new SportUnsettledBetCouchbase(rawData, betHistory, traceId, ResultType.BET.code, couchbaseId);

            SportsUnsettleBet sportsUnsettleBet = new SportsUnsettleBet(betHistory);
            WalletBalanceVo balanceVo = sportUnsettleAction.call(traceId, sportsUnsettleBet, httpRequestLog, betHistory);
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balanceVo.getData().getBalance());
            sportUnsettledBetCouchbase.setNewBetAmount(sportUnsettledBetCouchbase.getBetAmount());
            sportUnsettledBetCouchbase.setResettleNum((sportUnsettledBetCouchbase.getResettleNum() != null && sportUnsettledBetCouchbase.getResettleNum() >= 0) ? sportUnsettledBetCouchbase.getResettleNum() + 1 : 0);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);

            // Insert new unsettled bet into maria db
            SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB);
            
            sportsUnsettleBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportsUnsettleBet.setBalance(balanceVo.getData().getBalance());
            betEvent = new BetEvent(sportsUnsettleBet, balanceVo.getData().getBalance());

            // Generate new bet history to offset the old records
            betHistory = this.offsetOldBetHistory(betHistory);
            kafkaService.produceBetHistory(betHistory, null, BigDecimal.ONE);
    
        } catch (Exception e) {
            throw new InvalidOperatorResponseException();
        }
    
        return betEvent;
    }

    public BetEvent resettle(String traceId, SportUnsettleData sportUnsettleData, SportBetResultData sportBetResultData, String rawData, HttpRequestLog httpRequestLog) throws InvalidOperatorResponseException {
        BetEvent betEvent = null;

        try {
            this.unsettle(traceId, sportUnsettleData, rawData, httpRequestLog);
            betEvent = this.settle(traceId, sportBetResultData, httpRequestLog);

        } catch (Exception ex) {
            throw new InvalidOperatorResponseException();

        }

        return betEvent;
    }

    private BetHistory offsetOldBetHistory(BetHistory betHistory) {
        BigDecimal newBetAmount = Optional.ofNullable(betHistory.getBetAmount()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);
        BigDecimal newWinAmount = Optional.ofNullable(betHistory.getWinAmount()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);
        BigDecimal newWinLoss = Optional.ofNullable(betHistory.getWinLoss()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);
        BigDecimal newEffectiveTurnover = Optional.ofNullable(betHistory.getEffectiveTurnover()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);        
        Integer newResettleNum = Optional.ofNullable(betHistory.getResettleNum()).orElse(0);

        betHistory.setBetAmount(newBetAmount);
        betHistory.setWinAmount(newWinAmount);
        betHistory.setWinLoss(newWinLoss);
        betHistory.setEffectiveTurnover(newEffectiveTurnover);
        betHistory.setResettleNum(newResettleNum + 1);
        betHistory.setResultType(BetResultType.BET.code);

        return betHistory;
    }
}