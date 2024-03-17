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

    public BetEvent placeBet(String traceId, GameSession gameSession, SportBetResultData sportBetResultData, String rawData, HttpRequestLog httpRequestLog) throws InsufficientBalanceException, InvalidOperatorResponseException, BetResultIdempotentViolationException {

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

        sportUnsettledBetService.idempotentCheck(gameSession.getVendorPlayerUsername(), sportBetResultData.getExternalTransactionId());
        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = new SportUnsettledBetCouchbase(gameSession, rawData, sportBetResultData, traceId, ResultType.BET.code);
        BetEvent betEvent = null;

        try {
            WalletBalanceVo balanceVo = sportBetAction.call(traceId, gameSession, sportUnsettledBetCouchbase, httpRequestLog);
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balanceVo.getData().getBalance());
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);

            VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new VendorGame.SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB);

            betEvent = new BetEvent(sportUnsettledBetCouchbase, null);

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

    public BetEvent confirmBet(String traceId, GameSession gameSession, SportBetResultData sportBetResultData, String rawData, HttpRequestLog httpRequestLog) throws BetNotFoundException, BetResultIdempotentViolationException, InvalidOperatorResponseException, InsufficientBalanceException {

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
        if (sportUnsettledBetCouchbase.getIsConfirmBet() == 1) throw new BetResultIdempotentViolationException();
        // Update Bet Parameter
        sportUnsettledBetCouchbase.setNewBetAmount(sportBetResultData.getNewBetAmount());
        sportUnsettledBetCouchbase.setVendorBetId(sportBetResultData.getVendorBetId());

        if (sportUnsettledBetCouchbase.getOperatorStatus() == ResponseCodes.Status.SC_OK.code) {
            sportUnsettledBetCouchbase.setInternalTransactionId(traceId);
        }

        loggingService.logProcessTime("processBet ｜ unsettledBetService.idempotentCheck", traceId);

        BetEvent betEvent = null;
        try {
            // record operator processing time
            WalletBalanceVo balanceVo = sportUpdateBetAction.call(traceId, gameSession, sportUnsettledBetCouchbase, httpRequestLog);
            BigDecimal balance = balanceVo.getData().getBalance();

            // Update record in sport_unsettled_bet (Couchbase)
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balance);
            sportUnsettledBetCouchbase.setIsConfirmBet(1);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);

            // Update record in sport_unsettled_bet (MariaDB)
            VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new VendorGame.SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
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

    public BetEvent settle(String traceId, SportBetResultData sportBetResultData, HttpRequestLog httpRequestLog) throws BetNotFoundException, InvalidAgentApiCredentialException, RecordNotFoundException, InvalidOperatorResponseException, BetResultIdempotentViolationException {

        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportUnsettledBetService.couchbaseGetByExternalTransactionId(sportBetResultData.getVendorPlayerUsername(), sportBetResultData.getExternalTransactionId());
        sportUnsettledBetCouchbase.setInternalTransactionId(traceId);
        String unsettledBetId = sportUnsettledBetCouchbase.getBetId();
        Integer isResettlementBet = 0;
        BetEvent betEvent = null;

        try {
            SportSettledBet sportSettledBet = sportSettledBetService.getByExternalTransactionId(sportBetResultData.getVendorPlayerUsername(), sportBetResultData.getExternalTransactionId());

            if (sportBetResultData.getNewBetAmount() == sportSettledBet.getNewBetAmount() && sportBetResultData.getWinAmount() == sportSettledBet.getWinAmount()) {
                //check is idempotent for sportSettledBet
                throw new BetResultIdempotentViolationException("Cannot find sportSettledBet couchbase Id: " + sportBetResultData.getVendorPlayerUsername() + '_' + sportBetResultData.getExternalTransactionId());

            } else {
                //if record is found but newBetAmount and winAmount is not same with sportBetResultData, then it is a resettleBet then set a new betId for it.
                isResettlementBet = 1;

            }

        } catch (BetNotFoundException e) {
            //betNotFound would be one of the correct Behavior
            //If the bet is not found in sportSettledBet, then the bet should continue and settle as usual.

        }

        if (sportUnsettledBetCouchbase.getOperatorStatus() == ResponseCodes.Status.SC_OK.code) {
            //operatorStatus is ok means this unsettled is newly and going to settle, and proceed with default calculation
            sportUnsettledBetCouchbase.setWinAmount(sportBetResultData.getWinAmount());
            BigDecimal newBetAmount = sportUnsettledBetCouchbase.getNewBetAmount() != null ? sportUnsettledBetCouchbase.getNewBetAmount() : sportUnsettledBetCouchbase.getBetAmount();
            sportUnsettledBetCouchbase.setWinLoss(sportBetResultData.getWinAmount().subtract(newBetAmount));
            sportUnsettledBetCouchbase.setEffectiveTurnover(newBetAmount);
            sportUnsettledBetCouchbase.setResettleNum((sportUnsettledBetCouchbase.getResettleNum() != null && sportUnsettledBetCouchbase.getResettleNum() > 0) ? sportUnsettledBetCouchbase.getResettleNum() + 1 : 0);

            if (isResettlementBet == 1 || sportUnsettledBetCouchbase.getResultType().equals(BetResultType.ADJUSTMENT.code)) {
                //if its resettled or the bet is unsettled from settle bet (betResultType = Adjustment), then should generate as a new betId
                sportUnsettledBetCouchbase.setBetId(traceId);

            }

        } else {
            //If operatorStatus is not OK, which mean is a resend request, the data should not be updated again and send with same betId but different traceId
        }

        try {
            WalletBalanceVo balanceVo = sportSettleAction.call(traceId, sportUnsettledBetCouchbase, sportUnsettledBetCouchbase, httpRequestLog);
            betEvent = new BetEvent(sportUnsettledBetCouchbase, null);

            // Insert settled bet into bet_history (MariaDB)
            Integer betStatus = BetStatus.SETTLED.code;
            BigDecimal winAmount = sportBetResultData.getWinAmount();
            int resultType = winAmount.compareTo(BigDecimal.ZERO) > 0 ? BetResultType.WIN.code : BetResultType.LOSE.code;
            BetHistory betHistory = sportUnsettledBetCouchbase.toBetHistory(betStatus, resultType);
            kafkaService.produceBetHistory(betHistory, null, BigDecimal.ONE);

            // Insert record into sport_settled_bet (Couchbase)
            sportSettledBetService.save(new SportSettledBet(sportUnsettledBetCouchbase));

            //this to handle sportUnsettledBetCouchbase as settledBet with newId to send to operator but set back old betId to handle delete unsettledBet
            sportUnsettledBetCouchbase.setBetId(unsettledBetId);

            // Delete record in sport_unsettled_bet (Couchbase)
            sportUnsettledBetService.delete(sportUnsettledBetCouchbase);

            // Update status in sport_unsettled_bet (MariaDB)
            VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new VendorGame.SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
            sportUnsettledBetMariaDB.setStatus(1);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB);

        } catch (Exception e) {
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
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
            InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, BetNotFoundException, BetRefundIdempotentViolationException {

        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportUnsettledBetService.couchbaseGetByExternalTransactionId(sportRefundData.getVendorPlayerUsername(), sportRefundData.getExternalTransactionId());
        if (sportUnsettledBetCouchbase.getStatus().compareTo(BetStatus.REFUNDED.code) == 0)
            throw new BetRefundIdempotentViolationException();

        if (sportUnsettledBetCouchbase.getOperatorStatus() == ResponseCodes.Status.SC_OK.code) {
            sportUnsettledBetCouchbase.setInternalTransactionId(traceId);
        }

        BetEvent betEvent = null;
        Integer betStatus = BetStatus.REFUNDED.code;

        try {
            WalletBalanceVo balanceVo = sportRefundAction.call(traceId, sportUnsettledBetCouchbase, httpRequestLog);
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balanceVo.getData().getBalance());
            sportUnsettledBetCouchbase.setStatus(betStatus);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            betEvent = new BetEvent(sportUnsettledBetCouchbase, balanceVo.getData().getBalance());

        } catch (Exception e) {
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);
            throw new InvalidOperatorResponseException();

        }

        if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

        VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new VendorGame.SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
        kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB);

        // Todo decide move unsettled_couchbase to settled_couchbase

        BetHistory betHistory = sportUnsettledBetCouchbase.toBetHistory(betStatus, BetResultType.BET.code);
        kafkaService.produceBetHistory(betHistory, null, BigDecimal.ONE);

        return betEvent;
    }

    public BetEvent unsettle(String traceId, SportUnsettleData sportUnsettleData, String rawData, HttpRequestLog httpRequestLog) throws VendorCurrencyNotSupportException,
            InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, BetNotFoundException, InvalidPlayerException {

        BetEvent betEvent = null;
        SportSettledBet sportSettledBet = sportSettledBetService.getByExternalTransactionId(sportUnsettleData.getVendorPlayerUsername(), sportUnsettleData.getExternalTransactionId());
        sportSettledBet.setBetId(traceId);

        try {
            SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportSettledBet.toSportUnsettleBetCouchbase();

            if (sportUnsettledBetCouchbase.getOperatorStatus() == ResponseCodes.Status.SC_OK.code) {
                sportUnsettledBetCouchbase.setInternalTransactionId(traceId);
            }

            WalletBalanceVo balanceVo = sportUnsettleAction.call(traceId, sportUnsettledBetCouchbase, httpRequestLog);
            sportUnsettledBetCouchbase.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetCouchbase.setBalance(balanceVo.getData().getBalance());
            sportUnsettledBetCouchbase.setNewBetAmount(sportUnsettledBetCouchbase.getBetAmount());
            sportUnsettledBetCouchbase.setResultType(BetResultType.ADJUSTMENT.code);
            sportUnsettledBetCouchbase.setResettleNum((sportUnsettledBetCouchbase.getResettleNum() != null && sportUnsettledBetCouchbase.getResettleNum() >= 0) ? sportUnsettledBetCouchbase.getResettleNum() + 1 : 0);

            // Update status in (MariaDB)
            VendorGame.SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new VendorGame.SportUnsettledBetMariaDB(sportUnsettledBetCouchbase);
            sportUnsettledBetMariaDB.setStatus(BetStatus.UNSETTLED.code);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB);

            // Generate new bet history to offset the old records
            BetHistory betHistory = this.offsetOldBetHistory(sportUnsettledBetCouchbase.toBetHistory(BetStatus.UNSETTLED.code, BetResultType.ADJUSTMENT.code));
            kafkaService.produceBetHistory(betHistory, null, BigDecimal.ONE);

            // Delete data from couchbase settled bet
            sportSettledBetService.delete(sportSettledBet);

            // update unsettledBet with winAmount, winLoss and effectiveTurnover = 0
            sportUnsettledBetCouchbase.setWinAmount(BigDecimal.ZERO);
            sportUnsettledBetCouchbase.setWinLoss(BigDecimal.ZERO);
            sportUnsettledBetCouchbase.setEffectiveTurnover(BigDecimal.ZERO);
            sportUnsettledBetService.save(sportUnsettledBetCouchbase);

            betEvent = new BetEvent(sportUnsettledBetCouchbase, balanceVo.getData().getBalance());

        } catch (Exception e) {
            throw new InvalidOperatorResponseException();

        }

        return betEvent;
    }

    public BetEvent resettle(String traceId, SportResettleData sportResettleData, HttpRequestLog httpRequestLog) throws InvalidOperatorResponseException, BetNotFoundException {
        BetEvent betEvent = null;
        SportSettledBet sportSettledBet = sportSettledBetService.getByExternalTransactionId(sportResettleData.getVendorPlayerUsername(), sportResettleData.getExternalTransactionId());
        sportSettledBet.setBetId(traceId);

        try {
            WalletBalanceVo balanceVo = sportResettleAction.call(traceId, sportSettledBet, sportResettleData, httpRequestLog);

            BigDecimal diffWinAmount = sportResettleData.getNewWinAmount().subtract(sportSettledBet.getWinAmount());

            sportSettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportSettledBet.setBalance(balanceVo.getData().getBalance());
            sportSettledBet.setWinAmount(sportResettleData.getNewWinAmount());
            sportSettledBet.setResettleNum((sportSettledBet.getResettleNum() != null && sportSettledBet.getResettleNum() >= 0) ? sportSettledBet.getResettleNum() + 1 : 0);
            sportSettledBetService.save(sportSettledBet);

            betEvent = new BetEvent(sportSettledBet, balanceVo.getData().getBalance());

            // Generate new bet history to offset the old records
            int resultType = sportSettledBet.getWinAmount().compareTo(BigDecimal.ZERO) > 0 ? BetResultType.WIN.code : BetResultType.LOSE.code;
            BetHistory betHistory = sportSettledBet.toBetHistory(BetStatus.SETTLED.code, resultType);
            betHistory.setBetAmount(BigDecimal.ZERO);
            betHistory.setWinAmount(diffWinAmount);
            betHistory.setWinLoss(diffWinAmount);
            betHistory.setEffectiveTurnover(BigDecimal.ZERO);
            kafkaService.produceBetHistory(betHistory, null, BigDecimal.ONE);

        } catch (Exception e) {
            throw new InvalidOperatorResponseException();

        }

        return betEvent;
    }

    public BetEvent adjustment(String traceId, SportAdjustmentData sportAdjustmentData, HttpRequestLog httpRequestLog) throws InvalidOperatorResponseException, BetNotFoundException, TransactionStillProcessingException, BetAdjustmentIdempotentViolationException, InvalidPlayerException, RecordNotFoundException, VendorCurrencyNotSupportException, InsufficientBalanceException {

        // Todo rename to proper name (wallet adjustment)
        // Direct adjust player balance

        BetEvent betEvent = null;

        // get VendorPlayer
        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(sportAdjustmentData.getVendorUsername());
        AgentPlayer agentPlayer = agentPlayerService.get(vendorPlayer.getAgentPlayerId());

        // check idempotent
        sportBetAdjustmentLogService.idempotentCheck(traceId, vendorPlayer.getId().toString(), sportAdjustmentData.getExternalTransactionId());

        try {
            SportSettledBet sportSettledBet = new SportSettledBet(traceId, vendorPlayer, agentPlayer, sportAdjustmentData, httpRequestLog.getRequestBody());

            // Adjustment Request to Operator
            WalletBalanceVo balanceVo = sportAdjustmentAction.call(traceId, agentPlayer.getAgentId(), sportSettledBet, httpRequestLog);

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
            kafkaService.produceBetHistory(betHistory, null, BigDecimal.ONE);

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