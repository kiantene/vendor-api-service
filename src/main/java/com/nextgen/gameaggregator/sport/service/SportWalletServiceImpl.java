package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentAction;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentData;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundAction;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.operator.sport.resettle.SportResettleAction;
import com.nextgen.gameaggregator.operator.sport.resettle.SportResettleData;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.operator.sport.settle.SportSettleAction;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleAction;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleData;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.processor.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class SportWalletServiceImpl implements SportWalletService {

    private final AgentPlayerService agentPlayerService;
    private final KafkaService kafkaService;
    private final SportAdjustmentAction sportAdjustmentAction;
    private final SportBetAdjustmentLogService sportBetAdjustmentLogService;
    private final SportResettleAction sportResettleAction;
    private final SportSettleAction sportSettleAction;
    private final SportSettledBetService sportSettledBetService;
    private final SportUnsettledBetService sportUnsettledBetService;
    private final SportResettleBetProcessor sportResettleBetProcessor;
    private final SportRefundAction sportRefundAction;
    private final SportUnsettleAction sportUnsettleAction;
    private final VendorPlayerService vendorPlayerService;
    private final VendorService vendorService;

    private final SportSingleBetProcessor sportSingleBetProcessor;
    private final SportMultipleBetProcessor sportMultipleBetProcessor;
    private final SportUpdateBetProcessor sportUpdateBetProcessor;
    private final SportSettleBetProcessor sportSettleBetProcessor;
    private final SportUnsettleBetProcessor sportUnsettleBetProcessor;
    private final SportRefundProcessor sportRefundProcessor;

    @Autowired
    public SportWalletServiceImpl(AgentPlayerService agentPlayerService,
                                  KafkaService kafkaService,
                                  SportAdjustmentAction sportAdjustmentAction,
                                  SportBetAdjustmentLogService sportBetAdjustmentLogService,
                                  SportResettleAction sportResettleAction,
                                  SportSettleAction sportSettleAction,
                                  SportSettledBetService sportSettledBetService,
                                  SportUnsettledBetService sportUnsettledBetService,
                                  SportResettleBetProcessor sportResettleBetProcessor,
                                  SportRefundAction sportRefundAction,
                                  SportUnsettleAction sportUnsettleAction,
                                  VendorPlayerService vendorPlayerService,
                                  VendorService vendorService,
                                  SportSingleBetProcessor sportSingleBetProcessor,
                                  SportMultipleBetProcessor sportMultipleBetProcessor,
                                  SportUpdateBetProcessor sportUpdateBetProcessor,
                                  SportSettleBetProcessor sportSettleBetProcessor,
                                  SportUnsettleBetProcessor sportUnsettleBetProcessor,
                                  SportRefundProcessor sportRefundProcessor) {

        this.agentPlayerService = agentPlayerService;
        this.kafkaService = kafkaService;
        this.sportAdjustmentAction = sportAdjustmentAction;
        this.sportBetAdjustmentLogService = sportBetAdjustmentLogService;
        this.sportResettleAction = sportResettleAction;
        this.sportSettleAction = sportSettleAction;
        this.sportSettledBetService = sportSettledBetService;
        this.sportUnsettledBetService = sportUnsettledBetService;
        this.sportResettleBetProcessor = sportResettleBetProcessor;
        this.sportRefundAction = sportRefundAction;
        this.sportUnsettleAction = sportUnsettleAction;
        this.vendorPlayerService = vendorPlayerService;
        this.vendorService = vendorService;
        this.sportSingleBetProcessor = sportSingleBetProcessor;
        this.sportMultipleBetProcessor = sportMultipleBetProcessor;
        this.sportUpdateBetProcessor = sportUpdateBetProcessor;
        this.sportSettleBetProcessor = sportSettleBetProcessor;
        this.sportUnsettleBetProcessor = sportUnsettleBetProcessor;
        this.sportRefundProcessor = sportRefundProcessor;
    }

    @Override
    public WalletRequest placeBet(WalletRequest walletRequest) throws
            InsufficientBalanceException, InvalidOperatorResponseException,
            BetResultIdempotentViolationException, TransactionStillProcessingException, InvalidRequestException {

        return sportSingleBetProcessor.process(walletRequest);
    }

    @Override
    public WalletRequest placeMultipleBets(WalletRequest walletRequest) throws
            BetResultIdempotentViolationException, TransactionStillProcessingException,
            InvalidOperatorResponseException, InsufficientBalanceException, InvalidRequestException {

        return sportMultipleBetProcessor.process(walletRequest);
    }

    @Override
    public WalletRequest confirmBet(WalletRequest walletRequest) throws
            InvalidPlayerException, BetNotFoundException, BetNotAllowedException, BetResultIdempotentViolationException,
            InvalidOperatorResponseException, TransactionStillProcessingException, InvalidRequestException {

        return sportUpdateBetProcessor.process(walletRequest);
    }

    @Override
    public WalletRequest refund(WalletRequest walletRequest) throws
            BetNotFoundException, BetNotAllowedException, BetResultIdempotentViolationException,
            InvalidOperatorResponseException, TransactionStillProcessingException,
            InvalidPlayerException, InvalidRequestException {

        return sportRefundProcessor.process(walletRequest);
    }

    @Override
    public WalletRequest settle(WalletRequest walletRequest) throws
            BetNotFoundException, BetNotAllowedException, BetResultIdempotentViolationException,
            InvalidOperatorResponseException, TransactionStillProcessingException,
            InvalidRequestException, InvalidPlayerException {

        return sportSettleBetProcessor.process(walletRequest);
    }

    @Override
    @Deprecated
    public BetEvent settle(String traceId, SportBetResultData sportBetResultData, HttpRequestLog httpRequestLog) throws BetNotFoundException, InvalidAgentApiCredentialException, RecordNotFoundException, InvalidOperatorResponseException, BetResultIdempotentViolationException {

        String vendorPlayerUsername = sportBetResultData.getVendorPlayerUsername();
        String vendorBetId = sportBetResultData.getVendorBetId();

        if (httpRequestLog != null) {
            httpRequestLog.setRequestType(SportSettleAction.class.getSimpleName());
            httpRequestLog.setVendorBetId(sportBetResultData.getVendorBetId());
            httpRequestLog.setRoundId(sportBetResultData.getRoundId());
            httpRequestLog.setBetStart(System.currentTimeMillis());
        }

        SportUnsettledBet sportUnsettledBet = sportUnsettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);
        sportUnsettledBet.setInternalTransactionId(traceId);
        BetEvent betEvent = null;
        Integer resettleNum = 0;
        Integer unsettleResettleNum = 0;

        httpRequestLog.setVendorId(sportUnsettledBet.getVendorId());
        httpRequestLog.setVendorUsername(sportUnsettledBet.getVendorPlayerUsername());

        try {
            //idempotent checking on couchbase sport_settled_bet collection
            SportSettledBet sportSettledBet = sportSettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);

            //check is idempotent when externalTransactionId is matched
            if (sportSettledBet.getExternalTransactionId().equals(sportBetResultData.getExternalTransactionId())) {
                if (sportSettledBet.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
                    throw new BetResultIdempotentViolationException("Process settle idempotent: " + sportBetResultData.getVendorPlayerUsername() + '_' + sportBetResultData.getExternalTransactionId());
                } else {
                    sportUnsettledBet.setInternalTransactionId(sportSettledBet.getInternalTransactionId());
                }

            } else {
                // if settledBet is found but externalTransactionId is not matched, then is new status changed of this bet
                resettleNum = sportSettledBet.getResettleNum() + 1;
                unsettleResettleNum = sportSettledBet.getUnsettledResettleNum();
            }

        } catch (BetNotFoundException e) {
            //If the bet is not found in sportSettledBet, then the bet should continue and settle as usual.
        }

        BigDecimal newBetAmount = sportUnsettledBet.getNewBetAmount() != null ? sportUnsettledBet.getNewBetAmount() : sportUnsettledBet.getBetAmount();
        sportUnsettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        sportUnsettledBet.setWinAmount(sportBetResultData.getWinAmount());
        sportUnsettledBet.setWinLoss(sportBetResultData.getWinAmount().subtract(newBetAmount));
        sportUnsettledBet.setEffectiveTurnover(newBetAmount);
        sportUnsettledBet.setVendorSettleTime(Objects.requireNonNullElse(sportBetResultData.getVendorSettleTime(), System.currentTimeMillis()));
        sportUnsettledBet.setResultTime(Objects.requireNonNullElse(sportBetResultData.getResultTime(), sportUnsettledBet.getVendorSettleTime()));
        sportUnsettledBet.setExternalTransactionId(Objects.requireNonNullElse(sportBetResultData.getExternalTransactionId(), sportUnsettledBet.getExternalTransactionId()));

        try {
            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportUnsettledBet.getVendorId(), sportUnsettledBet.getCurrencyId());
            AgentPlayer agentPlayer = agentPlayerService.getByAgentPlayerId(sportUnsettledBet.getAgentPlayerId(), null);

            WalletBalanceVo balanceVo = sportSettleAction.call(traceId, sportUnsettledBet, httpRequestLog, vendorCurrency, agentPlayer);
            betEvent = new BetEvent(sportUnsettledBet, balanceVo.getData().getBalance());

            // Insert settled bet into bet_history (MariaDB)
            Integer betStatus = BetStatus.SETTLED.code;
            BigDecimal winAmount = sportBetResultData.getWinAmount();
            sportUnsettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBet.setResettleNum(resettleNum);
            sportUnsettledBet.setUnsettledResettleNum(unsettleResettleNum);
            int resultType = winAmount.compareTo(BigDecimal.ZERO) > 0 ? BetResultType.WIN.code : BetResultType.LOSE.code;

            // Insert record bet_history (MariaDB)
            BetHistory betHistory = sportUnsettledBet.toBetHistory(betStatus, resultType);
            kafkaService.produceBetHistory(betHistory, null, vendorCurrency.getFromVendorRate());

            kafkaService.produceWarehouseBetHistory
                    (betHistory, agentPlayer.getUsername(), sportUnsettledBet.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());

            // Update status in sport_unsettled_bet (MariaDB)
            SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(sportUnsettledBet);
            sportUnsettledBetMariaDB.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBetMariaDB.setResettleNum(unsettleResettleNum);
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB, vendorCurrency.getFromVendorRate());

            // Insert record into sport_settled_bet (Couchbase)
            SportSettledBet updatedSportSettledBet = new SportSettledBet(sportUnsettledBet);
            sportSettledBetService.save(updatedSportSettledBet);

            // Delete record in sport_unsettled_bet (Couchbase)
            sportUnsettledBetService.delete(sportUnsettledBet);

            // update master Unsettle bet if multiple bet
            if (Objects.nonNull(sportUnsettledBet.getMasterSportUnsettleBetId())) {
                Optional<SportUnsettledBet> sportMasterUnsettledBetOptional = sportUnsettledBetService.getById(sportUnsettledBet.getMasterSportUnsettleBetId());
                if (sportMasterUnsettledBetOptional.isPresent()) {
                    // Delete record in sport_unsettled_bet (Couchbase)
                    SportUnsettledBet masterUnsettledBet = sportMasterUnsettledBetOptional.get();
                    sportUnsettledBetService.delete(masterUnsettledBet);

                    // Update record in sport_master_unsettled_bet (MariaDB)
                    SportMasterUnsettledBetMariaDB sportMasterUnsettledBetMariaDB = new SportMasterUnsettledBetMariaDB(masterUnsettledBet);
                    sportMasterUnsettledBetMariaDB.setStatus(BetStatus.SETTLED.code);
                    kafkaService.produceMasterUnsettledBet(sportMasterUnsettledBetMariaDB, vendorCurrency.getFromVendorRate());
                }
            }

        } catch (Exception e) {
            sportUnsettledBet.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(sportUnsettledBet);
            sportSettledBetService.save(new SportSettledBet(sportUnsettledBet));
            throw new InvalidOperatorResponseException();

        }

        if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

        return betEvent;
    }

    @Override
    @Deprecated
    public BetEvent refund(String traceId, SportRefundData sportRefundData, HttpRequestLog httpRequestLog) throws VendorCurrencyNotSupportException,
            InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, BetNotFoundException, TransactionStillProcessingException, BetResultIdempotentViolationException, RecordNotFoundException {

        String externalTransactionId = sportRefundData.getExternalTransactionId();
        String vendorPlayerUsername = sportRefundData.getVendorPlayerUsername();
        String vendorBetId = sportRefundData.getVendorBetId();

        if (httpRequestLog != null) {
            httpRequestLog.setRequestType(SportRefundAction.class.getSimpleName());
            httpRequestLog.setBetStart(System.currentTimeMillis());
        }

        SportUnsettledBet sportUnsettledBet = sportUnsettledBetService.idempotentCheck(vendorPlayerUsername, vendorBetId, externalTransactionId);

        if (sportUnsettledBet == null) {
            //throw new BetNotFoundException();

            try {
                //idempotent checking on couchbase sport_settled_bet collection
                SportSettledBet sportSettledBet = sportSettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);
                String sportSettledBetId = vendorPlayerUsername + '_' + externalTransactionId;

                //check is idempotent when externalTransactionId is matched
                if (sportSettledBet.getExternalTransactionId().equals(externalTransactionId)) {
                    throw new BetResultIdempotentViolationException("Process refund idempotent: " + sportSettledBetId);

                } else {
                    //if settledBet is found but externalTransactionId is not matched, then is considered bet not found for refund
                    throw new BetNotFoundException("Process refund - settledBet is found with same round, but different externalTransactionId : " + sportSettledBetId);

                }

            } catch (BetNotFoundException e) {
                //If the bet is not found in sportSettledBet, which mean bet is totally not exists
                throw new BetNotFoundException(e.getMessage());

            }
        }

        // if externalTransactionId is not matched then will be using new internalTransactionId
        if (!sportUnsettledBet.getExternalTransactionId().equals(externalTransactionId)) {
            sportUnsettledBet.setInternalTransactionId(traceId);
        }

        sportUnsettledBet.setVendorSettleTime(Objects.requireNonNullElse(sportRefundData.getTimestamp(), System.currentTimeMillis()));
        sportUnsettledBet.setResultTime(sportUnsettledBet.getVendorSettleTime());
        sportUnsettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        sportUnsettledBet.setExternalTransactionId(Objects.requireNonNullElse(externalTransactionId, sportUnsettledBet.getExternalTransactionId()));

        httpRequestLog.setVendorId(sportUnsettledBet.getVendorId());
        httpRequestLog.setVendorBetId(sportUnsettledBet.getVendorBetId());
        httpRequestLog.setRoundId(sportUnsettledBet.getRoundId());
        httpRequestLog.setGameToken(sportUnsettledBet.getGameSessionToken());
        httpRequestLog.setBetStart(System.currentTimeMillis());
        httpRequestLog.setVendorUsername(sportUnsettledBet.getVendorPlayerUsername());


        BetEvent betEvent = null;
        Integer betStatus = BetStatus.REFUNDED.code;
        VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportUnsettledBet.getVendorId(), sportUnsettledBet.getCurrencyId());
        AgentPlayer agentPlayer = agentPlayerService.getByAgentPlayerId(sportUnsettledBet.getAgentPlayerId(), null);
        try {
            WalletBalanceVo balanceVo = sportRefundAction.call(traceId, sportUnsettledBet, httpRequestLog, vendorCurrency, agentPlayer);
            sportUnsettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBet.setBalance(balanceVo.getData().getBalance());
            sportUnsettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBet.setEffectiveTurnover(Objects.requireNonNullElse(sportUnsettledBet.getNewBetAmount(), sportUnsettledBet.getBetAmount()));
            sportUnsettledBet.setResettleNum((sportUnsettledBet.getResettleNum() != null && sportUnsettledBet.getResettleNum() > 0) ? sportUnsettledBet.getResettleNum() + 1 : 0);
            sportUnsettledBetService.save(sportUnsettledBet);
            betEvent = new BetEvent(sportUnsettledBet, balanceVo.getData().getBalance());

        } catch (Exception e) {
            sportUnsettledBet.setOperatorStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportUnsettledBetService.save(sportUnsettledBet);
            throw new InvalidOperatorResponseException();

        }

        if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

        // Insert record bet_history (MariaDB)
        BetHistory betHistory = sportUnsettledBet.toBetHistory(betStatus, BetResultType.BET.code);
        kafkaService.produceBetHistory(betHistory, null, vendorCurrency.getFromVendorRate());


        kafkaService.produceWarehouseBetHistory
                (betHistory, agentPlayer.getUsername(), sportRefundData.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());

        // Insert record into sport_settled_bet (Couchbase)
        sportSettledBetService.save(new SportSettledBet(sportUnsettledBet));

        // Delete record in sport_unsettled_bet (Couchbase)
        sportUnsettledBetService.delete(sportUnsettledBet);

        // Update status in sport_unsettled_bet (MariaDB)
        SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(sportUnsettledBet);
        sportUnsettledBetMariaDB.setResettleNum(sportUnsettledBet.getUnsettledResettleNum());
        kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB, vendorCurrency.getFromVendorRate());

        // update master Unsettle bet if multiple bet
        if (Objects.nonNull(sportUnsettledBet.getMasterSportUnsettleBetId())) {
            Optional<SportUnsettledBet> sportMasterUnsettledBetOptional = sportUnsettledBetService.getById(sportUnsettledBet.getMasterSportUnsettleBetId());
            if (sportMasterUnsettledBetOptional.isPresent()) {
                // Delete record in sport_unsettled_bet (Couchbase)
                SportUnsettledBet masterUnsettledBet = sportMasterUnsettledBetOptional.get();
                sportUnsettledBetService.delete(masterUnsettledBet);

                // Update record in sport_master_unsettled_bet (MariaDB)
                SportMasterUnsettledBetMariaDB sportMasterUnsettledBetMariaDB = new SportMasterUnsettledBetMariaDB(masterUnsettledBet);
                sportMasterUnsettledBetMariaDB.setStatus(BetStatus.SETTLED.code);
                kafkaService.produceMasterUnsettledBet(sportMasterUnsettledBetMariaDB, vendorCurrency.getFromVendorRate());
            }
        }

        return betEvent;
    }

    @Override
    public WalletRequest unsettle(WalletRequest walletRequest) throws
            BetNotFoundException, BetNotAllowedException, BetResultIdempotentViolationException,
            InvalidOperatorResponseException, TransactionStillProcessingException,
            InvalidRequestException, InvalidPlayerException {

        return sportUnsettleBetProcessor.process(walletRequest);
    }

    @Override
    @Deprecated
    public BetEvent unsettle(String traceId, SportUnsettleData sportUnsettleData, String rawData, HttpRequestLog httpRequestLog) throws VendorCurrencyNotSupportException,
            InsufficientBalanceException, InvalidOperatorResponseException, InvalidAgentApiCredentialException, BetNotFoundException, InvalidPlayerException, BetResultIdempotentViolationException {

        String externalTransactionId = sportUnsettleData.getExternalTransactionId();
        String vendorPlayerUsername = sportUnsettleData.getVendorPlayerUsername();
        String vendorBetId = sportUnsettleData.getVendorBetId();

        if (httpRequestLog != null) {
            httpRequestLog.setRequestType(SportUnsettleAction.class.getSimpleName());
            httpRequestLog.setBetStart(System.currentTimeMillis());
        }

        BetEvent betEvent = null;
        String internalTransactionId = traceId;
        SportSettledBet sportSettledBet = sportSettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);

        //check is idempotent when externalTransactionId is matched
        if (sportSettledBet.getExternalTransactionId().equals(externalTransactionId)) {
            if (sportSettledBet.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
                throw new BetResultIdempotentViolationException("Process unsettle idempotent: " + vendorPlayerUsername + '_' + externalTransactionId);
            } else {
                internalTransactionId = sportSettledBet.getInternalTransactionId();
            }

        } else {
            //if settledBet is found but externalTransactionId is not matched, then is new status changed of this bet
        }

        try {
            SportUnsettledBet sportUnsettledBet = sportSettledBet.toSportUnsettleBetCouchbase();
            sportUnsettledBet.setExternalTransactionId(Objects.requireNonNullElse(externalTransactionId, sportUnsettledBet.getExternalTransactionId()));
            sportUnsettledBet.setInternalTransactionId(internalTransactionId);
            sportUnsettledBet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
            Optional.ofNullable(sportUnsettleData.getTimestamp()).ifPresent(timestamp -> {
                sportUnsettledBet.setResultTime(timestamp);
                sportUnsettledBet.setVendorSettleTime(timestamp);
            });

            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportUnsettledBet.getVendorId(), sportUnsettledBet.getCurrencyId());
            AgentPlayer agentPlayer = agentPlayerService.getByAgentPlayerId(sportUnsettledBet.getAgentPlayerId(), null);

            WalletBalanceVo balanceVo = sportUnsettleAction.call(traceId, sportUnsettledBet, httpRequestLog, vendorCurrency, agentPlayer);
            sportUnsettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBet.setBalance(balanceVo.getData().getBalance());
            sportUnsettledBet.setResultType(BetResultType.ADJUSTMENT.code);
            sportUnsettledBet.setStatus(ResponseCodes.Status.SC_OK.code);
            sportUnsettledBet.setResettleNum((sportUnsettledBet.getResettleNum() != null && sportUnsettledBet.getResettleNum() >= 0) ? sportUnsettledBet.getResettleNum() + 1 : 0);
            sportUnsettledBet.setUnsettledResettleNum(this.getUnsettledBetResettleNum(sportSettledBet));

            // Update status in (MariaDB) sport_unsettled_bet
            SportUnsettledBetMariaDB sportUnsettledBetMariaDB = new SportUnsettledBetMariaDB(sportUnsettledBet);
            sportUnsettledBetMariaDB.setStatus(0);
            sportUnsettledBetMariaDB.setResettleNum(this.getUnsettledBetResettleNum(sportSettledBet));
            kafkaService.produceUnsettledBet(sportUnsettledBetMariaDB);

            // Generate new bet history to offset the old records
            BetHistory betHistory = this.offsetOldBetHistory(sportUnsettledBet.toBetHistory(BetStatus.CANCELLED.code, BetResultType.ADJUSTMENT.code));
            kafkaService.produceBetHistory(betHistory, null, vendorCurrency.getFromVendorRate());


            kafkaService.produceWarehouseBetHistory
                    (betHistory, agentPlayer.getUsername(), sportUnsettleData.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());

            // update data from couchbase settled bet
            sportSettledBet.setInternalTransactionId(internalTransactionId);
            sportSettledBet.setExternalTransactionId(sportUnsettleData.getExternalTransactionId());
            sportSettledBet.setWinAmount(BigDecimal.ZERO);
            sportSettledBet.setWinLoss(BigDecimal.ZERO);
            sportSettledBet.setEffectiveTurnover(BigDecimal.ZERO);
            sportSettledBet.setResettleNum((sportSettledBet.getResettleNum() != null && sportSettledBet.getResettleNum() >= 0) ? sportSettledBet.getResettleNum() + 1 : 0);
            sportSettledBet.setUnsettledResettleNum(sportUnsettledBet.getUnsettledResettleNum());
            sportSettledBetService.save(sportSettledBet);

            // update unsettledBet with winAmount, winLoss and effectiveTurnover = 0
            sportUnsettledBet.setWinAmount(BigDecimal.ZERO);
            sportUnsettledBet.setWinLoss(BigDecimal.ZERO);
            sportUnsettledBet.setEffectiveTurnover(BigDecimal.ZERO);
            sportUnsettledBetService.save(sportUnsettledBet);

            betEvent = new BetEvent(sportUnsettledBet, balanceVo.getData().getBalance());

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

        if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

        return betEvent;
    }

    @Override
    public WalletRequest resettle(WalletRequest walletRequest) throws
            BetNotFoundException, BetNotAllowedException, BetResultIdempotentViolationException,
            InvalidOperatorResponseException, TransactionStillProcessingException,
            InvalidRequestException, InvalidPlayerException {

        return sportResettleBetProcessor.process(walletRequest);
    }

    @Override
    @Deprecated(forRemoval = true)
    public BetEvent resettle(String traceId, SportResettleData sportResettleData, HttpRequestLog httpRequestLog) throws InvalidOperatorResponseException, BetNotFoundException, BetResultIdempotentViolationException {

        String vendorPlayerUsername = sportResettleData.getVendorPlayerUsername();
        String vendorBetId = sportResettleData.getVendorBetId();

        if (httpRequestLog != null) {
            httpRequestLog.setRequestType(SportResettleAction.class.getSimpleName());
            httpRequestLog.setBetStart(System.currentTimeMillis());
        }

        BetEvent betEvent = null;
        String internalTransactionId = traceId;
        SportSettledBet sportSettledBet = sportSettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);

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
        sportSettledBet.setExternalTransactionId(Objects.requireNonNullElse(sportResettleData.getExternalTransactionId(), sportSettledBet.getExternalTransactionId()));

        try {
            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportSettledBet.getVendorId(), sportSettledBet.getCurrencyId());
            AgentPlayer agentPlayer = agentPlayerService.getByAgentPlayerId(sportSettledBet.getAgentPlayerId(), null);

            WalletBalanceVo balanceVo = sportResettleAction.call(traceId, sportSettledBet, sportResettleData, httpRequestLog, vendorCurrency, agentPlayer);
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
            kafkaService.produceBetHistory(betHistory, null, vendorCurrency.getFromVendorRate());


            kafkaService.produceWarehouseBetHistory
                    (betHistory, agentPlayer.getUsername(), sportResettleData.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());


        } catch (Exception e) {
            sportSettledBet.setStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
            sportSettledBetService.save(sportSettledBet);
            throw new InvalidOperatorResponseException();

        }

        if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

        return betEvent;
    }

    @Override
    public BetEvent adjustment(String traceId, SportAdjustmentData sportAdjustmentData, HttpRequestLog httpRequestLog) throws InvalidOperatorResponseException, BetNotFoundException, TransactionStillProcessingException, BetAdjustmentIdempotentViolationException, InvalidPlayerException, RecordNotFoundException, VendorCurrencyNotSupportException, InsufficientBalanceException {

        if (httpRequestLog != null) {
            httpRequestLog.setRequestType(SportAdjustmentAction.class.getSimpleName());
            httpRequestLog.setBetStart(System.currentTimeMillis());
        }

        BetEvent betEvent = null;

        // get VendorPlayer
        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(sportAdjustmentData.getVendorUsername());
        AgentPlayer agentPlayer = agentPlayerService.getByAgentPlayerId(vendorPlayer.getAgentPlayerId(), null);

        // check idempotent
        sportBetAdjustmentLogService.idempotentCheck(traceId, vendorPlayer.getId().toString(), sportAdjustmentData.getExternalTransactionId());

        try {
            SportSettledBet sportSettledBet = new SportSettledBet(traceId, vendorPlayer, agentPlayer, sportAdjustmentData, httpRequestLog.getRequestBody());
            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(sportSettledBet.getVendorId(), sportSettledBet.getCurrencyId());

            // Adjustment Request to Operator
            WalletBalanceVo balanceVo = sportAdjustmentAction.call(traceId, agentPlayer.getAgentId(), sportSettledBet, httpRequestLog, vendorCurrency, agentPlayer);

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

            kafkaService.produceWarehouseBetHistory
                    (betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());


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

        if (httpRequestLog != null) httpRequestLog.setBetEnd(System.currentTimeMillis());

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

    private Integer getUnsettledBetResettleNum(SportSettledBet sportSettledBet) {

        Integer unsettledResettleNum = 0;

        if (sportSettledBet.getUnsettledResettleNum() != null) {
            unsettledResettleNum = sportSettledBet.getUnsettledResettleNum() + 1;
        }

        return unsettledResettleNum;

    }
}