package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentAction;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentData;
import com.nextgen.gameaggregator.operator.sport.bet.SportBetAction;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundAction;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.operator.sport.resettle.SportResettleAction;
import com.nextgen.gameaggregator.operator.sport.resettle.SportResettleData;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.operator.sport.settle.SportSettleAction;
import com.nextgen.gameaggregator.operator.sport.settle.SportWalletSettleAction;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleAction;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleData;
import com.nextgen.gameaggregator.operator.sport.updatebet.SportUpdateBetAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetAction;
import com.nextgen.gameaggregator.repository.ga.writer.BetHistoryRepository;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class SportWalletService {

    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private BetHistoryRepository betHistoryRepository;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private LoggingService loggingService;
    @Autowired
    private SportAdjustmentAction sportAdjustmentAction;
    @Autowired
    private SportBetAction sportBetAction;
    @Autowired
    private SportBetAdjustmentLogService sportBetAdjustmentLogService;
    @Autowired
    private SportResettleAction sportResettleAction;
    @Autowired
    private SportSettleAction sportSettleAction;
    @Autowired
    private SportSettledBetService sportSettledBetService;
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
    @Autowired
    private VendorService vendorService;

    public BetEvent placeBet(String traceId, GameSession gameSession, SportBetResultData sportBetResultData, String rawData, HttpRequestLog httpRequestLog) throws InsufficientBalanceException, InvalidOperatorResponseException, BetResultIdempotentViolationException, TransactionStillProcessingException {

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

        SportUnsettledBetCouchbase sportUnsettledBetCouchbaseOld = sportUnsettledBetService.idempotentCheck(gameSession.getVendorPlayerUsername(), sportBetResultData.getRoundId(), sportBetResultData.getExternalTransactionId());
        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = new SportUnsettledBetCouchbase(gameSession, rawData, sportBetResultData, traceId, ResultType.BET.code);
        sportUnsettledBetCouchbase.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        BetEvent betEvent = null;

        if (sportUnsettledBetCouchbaseOld != null) {
            sportUnsettledBetCouchbase.setBetId(sportUnsettledBetCouchbaseOld.getBetId());
            sportUnsettledBetCouchbase.setInternalTransactionId(sportUnsettledBetCouchbaseOld.getInternalTransactionId());

        }

        try {
            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportUnsettledBetCouchbase.getVendorId(), sportUnsettledBetCouchbase.getCurrencyId());
            WalletBalanceVo balanceVo = sportBetAction.call(traceId, gameSession, sportUnsettledBetCouchbase, httpRequestLog, vendorCurrency);
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balanceVo.getData().getBalance());
            sportUnsettledBetCouchbase.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);

            VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new VendorGame.SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB, vendorCurrency.getFromVendorRate());

            betEvent = new BetEvent(sportUnsettledBetCouchbase, balanceVo.getData().getBalance());

        } catch (InvalidOperatorResponseException e) {

            // record status code from operator if they return an error
            Integer operatorStatus = e.getOperatorStatus();
            sportUnsettledBetCouchbase.setOperatorStatus(operatorStatus);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);

            if (operatorStatus.equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                throw new InsufficientBalanceException();
            } else {
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

    public BetEvent confirmBet(String traceId, GameSession gameSession, SportBetResultData sportBetResultData, String rawData, HttpRequestLog httpRequestLog) throws BetNotFoundException, BetResultIdempotentViolationException, InvalidOperatorResponseException, InsufficientBalanceException, TransactionStillProcessingException {

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

        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportUnsettledBetService.idempotentCheck(gameSession.getVendorPlayerUsername(), sportBetResultData.getRoundId(), sportBetResultData.getExternalTransactionId());

        // if idempotent check is passed then set internalTransactionId as new traceId
        if (sportUnsettledBetCouchbase.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
            sportUnsettledBetCouchbase.setInternalTransactionId(traceId);
        }

        BigDecimal newBetAmount = Optional.ofNullable(sportBetResultData.getNewBetAmount()).orElse(Objects.requireNonNullElse(sportBetResultData.getBetAmount(), sportUnsettledBetCouchbase.getBetAmount()));
        sportUnsettledBetCouchbase.setNewBetAmount(newBetAmount);
        sportUnsettledBetCouchbase.setEffectiveTurnover(Objects.requireNonNullElse(sportBetResultData.getEffectiveTurnover(), newBetAmount));
        sportUnsettledBetCouchbase.setVendorBetId(sportBetResultData.getVendorBetId());
        Optional.ofNullable(sportBetResultData.getVendorBetTime()).ifPresent(sportUnsettledBetCouchbase::setVendorBetTime);
        sportUnsettledBetCouchbase.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        BetEvent betEvent = null;

        try {
            // record operator processing time
            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportUnsettledBetCouchbase.getVendorId(), sportUnsettledBetCouchbase.getCurrencyId());
            WalletBalanceVo balanceVo = sportUpdateBetAction.call(traceId, gameSession, sportUnsettledBetCouchbase, httpRequestLog, vendorCurrency);
            BigDecimal balance = balanceVo.getData().getBalance();

            // Update record in sport_unsettled_bet (Couchbase)
            sportUnsettledBetCouchbase.setStatus(0);
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balance);
            sportUnsettledBetCouchbase.setIsConfirmBet(1);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);

            // Update record in sport_unsettled_bet (MariaDB)
            VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new VendorGame.SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB, vendorCurrency.getFromVendorRate());

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

    public BetEvent settle(String traceId, SportBetResultData sportBetResultData, HttpRequestLog httpRequestLog) throws BetNotFoundException, InvalidAgentApiCredentialException, RecordNotFoundException, InvalidOperatorResponseException, BetResultIdempotentViolationException {

        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportUnsettledBetService.getByVendorPlayerUsernameAndRoundId(sportBetResultData.getVendorPlayerUsername(), sportBetResultData.getRoundId());
        sportUnsettledBetCouchbase.setInternalTransactionId(traceId);
        BetEvent betEvent = null;
        Integer resettle_num = 0;

        try {
            //idempotent checking on couchbase sport_settled_bet collection
            SportSettledBet sportSettledBet = sportSettledBetService.getByRoundId(sportBetResultData.getVendorPlayerUsername(), sportBetResultData.getRoundId());

            //check is idempotent when externalTransactionId is matched
            if (sportSettledBet.getExternalTransactionId().equals(sportBetResultData.getExternalTransactionId())) {
                if (sportSettledBet.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
                    throw new BetResultIdempotentViolationException("Process settle idempotent: " + sportBetResultData.getVendorPlayerUsername() + '_' + sportBetResultData.getExternalTransactionId());
                } else {
                    sportUnsettledBetCouchbase.setInternalTransactionId(sportSettledBet.getInternalTransactionId());
                }

            } else {
                //if settledBet is found but externalTransactionId is not matched, then is new status changed of this bet
                resettle_num = sportSettledBet.getResettleNum() + 1;
            }

        } catch (BetNotFoundException e) {
            //If the bet is not found in sportSettledBet, then the bet should continue and settle as usual.
        }

        BigDecimal newBetAmount = sportUnsettledBetCouchbase.getNewBetAmount() != null ? sportUnsettledBetCouchbase.getNewBetAmount() : sportUnsettledBetCouchbase.getBetAmount();
        sportUnsettledBetCouchbase.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        sportUnsettledBetCouchbase.setWinAmount(sportBetResultData.getWinAmount());
        sportUnsettledBetCouchbase.setWinLoss(sportBetResultData.getWinAmount().subtract(newBetAmount));
        sportUnsettledBetCouchbase.setEffectiveTurnover(newBetAmount);
        sportUnsettledBetCouchbase.setVendorSettleTime(Objects.requireNonNullElse(sportBetResultData.getVendorSettleTime(), System.currentTimeMillis()));
        sportUnsettledBetCouchbase.setResultTime(Objects.requireNonNullElse(sportBetResultData.getResultTime(), sportUnsettledBetCouchbase.getVendorSettleTime()));

        try {
            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportUnsettledBetCouchbase.getVendorId(), sportUnsettledBetCouchbase.getCurrencyId());
            WalletBalanceVo balanceVo = sportSettleAction.call(traceId, sportUnsettledBetCouchbase, httpRequestLog, vendorCurrency);
            betEvent = new BetEvent(sportUnsettledBetCouchbase, balanceVo.getData().getBalance());

            // Insert settled bet into bet_history (MariaDB)
            Integer betStatus = BetStatus.SETTLED.code;
            BigDecimal winAmount = sportBetResultData.getWinAmount();
            sportUnsettledBetCouchbase.setStatus(ResponseCodes.Status.SC_OK.code);
            int resultType = winAmount.compareTo(BigDecimal.ZERO) > 0 ? BetResultType.WIN.code : BetResultType.LOSE.code;

            // Insert record bet_history (MariaDB)
            BetHistory betHistory = sportUnsettledBetCouchbase.toBetHistory(betStatus, resultType);
            betHistory.setResettleNum(resettle_num);
            betHistory.setExternalTransactionId(Objects.requireNonNullElse(sportBetResultData.getExternalTransactionId(), betHistory.getExternalTransactionId()));
            kafkaService.produceBetHistory(betHistory, null, vendorCurrency.getFromVendorRate());

            // Update status in sport_unsettled_bet (MariaDB)
            VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new VendorGame.SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
            sportUnsettledBetMariaDB.setStatus(ResponseCodes.Status.SC_OK.code);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB, vendorCurrency.getFromVendorRate());

            // Insert record into sport_settled_bet (Couchbase)
            sportUnsettledBetCouchbase.setResettleNum(resettle_num);
            sportSettledBetService.save(new SportSettledBet(sportUnsettledBetCouchbase));

            // Delete record in sport_unsettled_bet (Couchbase)
            sportUnsettledBetService.delete(sportUnsettledBetCouchbase);

        } catch (Exception e) {
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            sportSettledBetService.save(new SportSettledBet(sportUnsettledBetCouchbase));
            throw new InvalidOperatorResponseException();

        }

        return betEvent;
    }

    @Async
    public void batchSettle(List<SportBetResultData> sportBetResultDataList) throws InvalidAgentApiCredentialException, RecordNotFoundException, BetNotFoundException, InvalidOperatorResponseException, BetResultIdempotentViolationException {
        for (SportBetResultData sportBetResultData : sportBetResultDataList) {
            String traceId = UUID.randomUUID().toString();
            this.settle(traceId, sportBetResultData, null);
        }
    }

    public BetEvent refund(String traceId, SportRefundData sportRefundData, String rawData, HttpRequestLog httpRequestLog) throws VendorCurrencyNotSupportException,
            InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, BetNotFoundException, BetRefundIdempotentViolationException, BetResultIdempotentViolationException, TransactionStillProcessingException {

        if (httpRequestLog != null) {
            httpRequestLog.setRequestType(SportRefundAction.class.getSimpleName());
            httpRequestLog.setBetStart(System.currentTimeMillis());
        }

        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportUnsettledBetService.idempotentCheck(sportRefundData.getVendorPlayerUsername(), sportRefundData.getRoundId(), sportRefundData.getExternalTransactionId());


        if (sportUnsettledBetCouchbase == null) {
            throw new BetNotFoundException();
        }

        // if externalTransactionId is not matched then will be using new internalTransactionId
        if (!sportUnsettledBetCouchbase.getExternalTransactionId().equals(sportRefundData.getExternalTransactionId())) {
            sportUnsettledBetCouchbase.setInternalTransactionId(traceId);
        }

        sportUnsettledBetCouchbase.setVendorSettleTime(Objects.requireNonNullElse(sportRefundData.getTimestamp(), System.currentTimeMillis()));
        sportUnsettledBetCouchbase.setResultTime(sportUnsettledBetCouchbase.getVendorSettleTime());
        sportUnsettledBetCouchbase.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);

        httpRequestLog.setVendorId(sportUnsettledBetCouchbase.getVendorId());
        httpRequestLog.setVendorBetId(sportUnsettledBetCouchbase.getVendorBetId());
        httpRequestLog.setRoundId(sportUnsettledBetCouchbase.getRoundId());
        httpRequestLog.setGameToken(sportUnsettledBetCouchbase.getGameSessionToken());
        httpRequestLog.setBetStart(System.currentTimeMillis());
        httpRequestLog.setVendorUsername(sportUnsettledBetCouchbase.getVendorPlayerUsername());

        BetEvent betEvent = null;
        Integer betStatus = BetStatus.REFUNDED.code;
        VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportUnsettledBetCouchbase.getVendorId(), sportUnsettledBetCouchbase.getCurrencyId());

        try {
            WalletBalanceVo balanceVo = sportRefundAction.call(traceId, sportUnsettledBetCouchbase, httpRequestLog, vendorCurrency);
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balanceVo.getData().getBalance());
            sportUnsettledBetCouchbase.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setEffectiveTurnover(Objects.requireNonNullElse(sportUnsettledBetCouchbase.getNewBetAmount(), sportUnsettledBetCouchbase.getBetAmount()));
            sportUnsettledBetCouchbase.setResettleNum((sportUnsettledBetCouchbase.getResettleNum() != null && sportUnsettledBetCouchbase.getResettleNum() > 0) ? sportUnsettledBetCouchbase.getResettleNum() + 1 : 0);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            betEvent = new BetEvent(sportUnsettledBetCouchbase, balanceVo.getData().getBalance());

        } catch (Exception e) {
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetCouchbase.setExternalTransactionId(Objects.requireNonNullElse(sportRefundData.getExternalTransactionId(), sportUnsettledBetCouchbase.getExternalTransactionId()));
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            throw new InvalidOperatorResponseException();

        }

        if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

        // Insert record bet_history (MariaDB)
        BetHistory betHistory = sportUnsettledBetCouchbase.toBetHistory(betStatus, BetResultType.BET.code);
        betHistory.setExternalTransactionId(Objects.requireNonNullElse(sportRefundData.getExternalTransactionId(), betHistory.getExternalTransactionId()));
        kafkaService.produceBetHistory(betHistory, null, vendorCurrency.getFromVendorRate());

        // Insert record into sport_settled_bet (Couchbase)
        sportSettledBetService.save(new SportSettledBet(sportUnsettledBetCouchbase));

        // Delete record in sport_unsettled_bet (Couchbase)
        sportUnsettledBetService.delete(sportUnsettledBetCouchbase);

        // Update status in sport_unsettled_bet (MariaDB)
        VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new VendorGame.SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
        kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB, vendorCurrency.getFromVendorRate());

        return betEvent;
    }

    public BetEvent unsettle(String traceId, SportUnsettleData sportUnsettleData, String rawData, HttpRequestLog httpRequestLog) throws VendorCurrencyNotSupportException,
            InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, BetNotFoundException, InvalidPlayerException, BetResultIdempotentViolationException {

        BetEvent betEvent = null;
        String internalTransactionId = traceId;
        SportSettledBet sportSettledBet = sportSettledBetService.getByRoundId(sportUnsettleData.getVendorPlayerUsername(), sportUnsettleData.getRoundId());

        //check is idempotent when externalTransactionId is matched
        if (sportSettledBet.getExternalTransactionId().equals(sportUnsettleData.getExternalTransactionId())) {
            if (sportSettledBet.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
                throw new BetResultIdempotentViolationException("Process unsettle idempotent: " + sportUnsettleData.getVendorPlayerUsername() + '_' + sportUnsettleData.getExternalTransactionId());
            } else {
                internalTransactionId = sportSettledBet.getInternalTransactionId();
            }

        } else {
            //if settledBet is found but externalTransactionId is not matched, then is new status changed of this bet
        }

        try {
            SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportSettledBet.toSportUnsettleBetCouchbase();
            sportUnsettledBetCouchbase.setInternalTransactionId(internalTransactionId);
            sportUnsettledBetCouchbase.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
            Optional.ofNullable(sportUnsettleData.getTimestamp()).ifPresent(timestamp -> {
                sportUnsettledBetCouchbase.setResultTime(timestamp);
                sportUnsettledBetCouchbase.setVendorSettleTime(timestamp);
            });

            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportUnsettledBetCouchbase.getVendorId(), sportUnsettledBetCouchbase.getCurrencyId());
            WalletBalanceVo balanceVo = sportUnsettleAction.call(traceId, sportUnsettledBetCouchbase, httpRequestLog, vendorCurrency);
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balanceVo.getData().getBalance());
            sportUnsettledBetCouchbase.setResultType(BetResultType.ADJUSTMENT.code);
            sportUnsettledBetCouchbase.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setResettleNum((sportUnsettledBetCouchbase.getResettleNum() != null && sportUnsettledBetCouchbase.getResettleNum() >= 0) ? sportUnsettledBetCouchbase.getResettleNum() + 1 : 0);

            // Update status in (MariaDB) sport_unsettled_bet
            VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new VendorGame.SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
            sportUnsettledBetMariaDB.setStatus(0);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB);

            // Generate new bet history to offset the old records
            BetHistory betHistory = this.offsetOldBetHistory(sportUnsettledBetCouchbase.toBetHistory(BetStatus.CANCELLED.code, BetResultType.ADJUSTMENT.code));
            betHistory.setExternalTransactionId(Objects.requireNonNullElse(sportUnsettleData.getExternalTransactionId(), betHistory.getExternalTransactionId()));
            kafkaService.produceBetHistory(betHistory, null, vendorCurrency.getFromVendorRate());

            // update data from couchbase settled bet
            sportSettledBet.setInternalTransactionId(internalTransactionId);
            sportSettledBet.setExternalTransactionId(sportUnsettleData.getExternalTransactionId());
            sportSettledBet.setWinAmount(BigDecimal.ZERO);
            sportSettledBet.setWinLoss(BigDecimal.ZERO);
            sportSettledBet.setEffectiveTurnover(BigDecimal.ZERO);
            sportSettledBet.setResettleNum((sportSettledBet.getResettleNum() != null && sportSettledBet.getResettleNum() >= 0) ? sportSettledBet.getResettleNum() + 1 : 0);
            sportSettledBetService.save(sportSettledBet);

            // update unsettledBet with winAmount, winLoss and effectiveTurnover = 0
            sportUnsettledBetCouchbase.setWinAmount(BigDecimal.ZERO);
            sportUnsettledBetCouchbase.setWinLoss(BigDecimal.ZERO);
            sportUnsettledBetCouchbase.setEffectiveTurnover(BigDecimal.ZERO);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);

            betEvent = new BetEvent(sportUnsettledBetCouchbase, balanceVo.getData().getBalance());

        } catch (InvalidOperatorResponseException e) {

            // record status code from operator if they return an error
            Integer operatorStatus = e.getOperatorStatus();
            sportSettledBet.setOperatorStatus(operatorStatus);
            sportSettledBet.setInternalTransactionId(internalTransactionId);
            sportSettledBetService.save(sportSettledBet);

            if (operatorStatus.equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                throw new InsufficientBalanceException();
            } else {
                throw e;
            }

        } catch (Exception e) {
            sportSettledBet.setStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportSettledBet.setInternalTransactionId(internalTransactionId);
            sportSettledBetService.save(sportSettledBet);
            throw new InvalidOperatorResponseException();

        }

        return betEvent;
    }

    public BetEvent resettle(String traceId, SportResettleData sportResettleData, HttpRequestLog httpRequestLog) throws InvalidOperatorResponseException, BetNotFoundException, BetResultIdempotentViolationException {
        BetEvent betEvent = null;
        String internalTransactionId = traceId;
        SportSettledBet sportSettledBet = sportSettledBetService.getByRoundId(sportResettleData.getVendorPlayerUsername(), sportResettleData.getRoundId());

        //check is idempotent when externalTransactionId is matched
        if (sportSettledBet.getExternalTransactionId() == sportResettleData.getExternalTransactionId()) {
            if (sportSettledBet.getStatus() == ResponseCodes.Status.SC_OK.code) {
                throw new BetResultIdempotentViolationException();
            } else {
                internalTransactionId = sportSettledBet.getInternalTransactionId();
            }

        } else {
            //if settledBet is found but externalTransactionId is not matched, then is new status changed of this bet
        }

        sportSettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        sportSettledBet.setInternalTransactionId(internalTransactionId);

        try {
            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportSettledBet.getVendorId(), sportSettledBet.getCurrencyId());
            WalletBalanceVo balanceVo = sportResettleAction.call(traceId, sportSettledBet, sportResettleData, httpRequestLog, vendorCurrency);
            BigDecimal diffWinAmount = sportResettleData.getNewWinAmount().subtract(sportSettledBet.getWinAmount());
            int resultType = diffWinAmount.compareTo(BigDecimal.ZERO) > 0 ? BetResultType.WIN.code : BetResultType.LOSE.code;

            sportSettledBet.setWinAmount(sportResettleData.getNewWinAmount());
            sportSettledBet.setWinLoss(sportSettledBet.getWinAmount());
            sportSettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportSettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
            sportSettledBet.setBalance(balanceVo.getData().getBalance());
            sportSettledBet.setResettleNum((sportSettledBet.getResettleNum() != null && sportSettledBet.getResettleNum() >= 0) ? sportSettledBet.getResettleNum() + 1 : 0);
            sportSettledBet.setResultType(sportSettledBet.getWinAmount().compareTo(BigDecimal.ZERO) > 0 ? BetResultType.WIN.code : BetResultType.LOSE.code);
            sportSettledBetService.save(sportSettledBet);

            betEvent = new BetEvent(sportSettledBet, balanceVo.getData().getBalance());

            // Generate new bet history to offset the old records
            BetHistory betHistory = sportSettledBet.toBetHistory(BetStatus.SETTLED.code, resultType);
            betHistory.setBetAmount(BigDecimal.ZERO);
            betHistory.setWinAmount(diffWinAmount);
            betHistory.setWinLoss(diffWinAmount);
            betHistory.setEffectiveTurnover(BigDecimal.ZERO);
            betHistory.setExternalTransactionId(Objects.requireNonNullElse(sportResettleData.getExternalTransactionId(), betHistory.getExternalTransactionId()));
            kafkaService.produceBetHistory(betHistory, null, vendorCurrency.getFromVendorRate());

        } catch (Exception e) {
            sportSettledBet.setStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportSettledBetService.save(sportSettledBet);
            throw new InvalidOperatorResponseException();

        }

        return betEvent;
    }

    public BetEvent adjustment(String traceId, SportAdjustmentData sportAdjustmentData, HttpRequestLog httpRequestLog) throws InvalidOperatorResponseException, BetNotFoundException, TransactionStillProcessingException, BetAdjustmentIdempotentViolationException, InvalidPlayerException, RecordNotFoundException, VendorCurrencyNotSupportException, InsufficientBalanceException {

        BetEvent betEvent = null;

        // get VendorPlayer
        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(sportAdjustmentData.getVendorUsername());
        AgentPlayer agentPlayer = agentPlayerService.get(vendorPlayer.getAgentPlayerId());

        // check idempotent
        sportBetAdjustmentLogService.idempotentCheck(traceId, vendorPlayer.getId().toString(), sportAdjustmentData.getExternalTransactionId());

        try {
            SportSettledBet sportSettledBet = new SportSettledBet(traceId, vendorPlayer, agentPlayer, sportAdjustmentData, httpRequestLog.getRequestBody());
            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportSettledBet.getVendorId(), sportSettledBet.getCurrencyId());

            // Adjustment Request to Operator
            WalletBalanceVo balanceVo = sportAdjustmentAction.call(traceId, agentPlayer.getAgentId(), sportSettledBet, httpRequestLog, vendorCurrency);

            // update operator status after receiving response from operator
            sportSettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportSettledBet.setBalance(balanceVo.getData().getBalance());
            sportSettledBetService.save(sportSettledBet);

            betEvent = new BetEvent(sportSettledBet, balanceVo.getData().getBalance());

            // update operator status after receiving response from operator
            RawBetAdjustmentLog rawBetAdjustmentLog = sportBetAdjustmentLogService.newSportBetAdjustmentLog(traceId, vendorPlayer, agentPlayer, sportAdjustmentData, balanceVo.getData().getBalance());
            rawBetAdjustmentLog.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            rawBetAdjustmentLog.setBalance(balanceVo.getData().getBalance());
            sportBetAdjustmentLogService.create(rawBetAdjustmentLog);

            // Generate new bet history to offset the old records
            BetHistory betHistory = sportSettledBet.toBetHistory(BetStatus.SETTLED.code, BetResultType.ADJUSTMENT.code);
            kafkaService.produceBetHistory(betHistory, null, vendorCurrency.getFromVendorRate());

        } catch (InvalidOperatorResponseException e) {

            Integer operatorStatus = e.getOperatorStatus();

            if (operatorStatus.equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                throw new InsufficientBalanceException();
            } else {
                throw e;
            }

        } catch (Exception e) {
            throw new InvalidOperatorResponseException();

        }

        return betEvent;
    }

    public void asyncSettle(SportBetResultData sportBetResultData) {
        try {
            SportRawSettledBet sportRawSettledBet = new SportRawSettledBet();
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
            modelMapper.map(sportBetResultData, sportRawSettledBet);
            kafkaService.produceRawSettledBet(sportRawSettledBet);

        } catch (Exception e) {

            // Todo error handling

        }
    }

    private BetHistory offsetOldBetHistory(BetHistory betHistory) {
        BigDecimal newBetAmount = Optional.ofNullable(betHistory.getBetAmount()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);
        BigDecimal newWinAmount = Optional.ofNullable(betHistory.getWinAmount()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);
        BigDecimal newWinLoss = Optional.ofNullable(betHistory.getWinLoss()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);
        BigDecimal newEffectiveTurnover = Optional.ofNullable(betHistory.getEffectiveTurnover()).map(BigDecimal::negate).orElse(BigDecimal.ZERO);

        betHistory.setBetAmount(newBetAmount);
        betHistory.setWinAmount(newWinAmount);
        betHistory.setWinLoss(newWinLoss);
        betHistory.setEffectiveTurnover(newEffectiveTurnover);

        return betHistory;
    }
}