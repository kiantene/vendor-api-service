package com.nextgen.gameaggregator.operator.wallet.betcredit;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.InternalServerException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletBetCreditProcessor {

    private final WalletRequestService walletRequestService;
    private final WalletTransactionService walletTransactionService;
    private final KafkaService kafkaService;
    private final SettledBetService settledBetService;
    private final BetResultLogService betResultLogService;
    private final BetRefundLogService betRefundLogService;

    public WalletBetCreditProcessor(WalletRequestService walletRequestService,
                                    WalletTransactionService walletTransactionService,
                                    KafkaService kafkaService,
                                    SettledBetService settledBetService,
                                    BetResultLogService betResultLogService,
                                    BetRefundLogService betRefundLogService) {
        this.walletRequestService = walletRequestService;
        this.walletTransactionService = walletTransactionService;
        this.kafkaService = kafkaService;
        this.settledBetService = settledBetService;
        this.betResultLogService = betResultLogService;
        this.betRefundLogService = betRefundLogService;
    }

    public WalletRequest process(WalletRequest walletRequest) throws InternalServerException, BetNotAllowedException, InvalidOperatorResponseException, BetResultIdempotentViolationException {

        walletRequestService.initialise(walletRequest);

        walletTransactionService.idempotentCheck(walletRequest, OperatorWalletService.CREDIT);

        WalletTransaction walletTransaction = walletTransactionService.prepareEntity(walletRequest, OperatorWalletService.CREDIT);

        walletTransactionService.save(walletTransaction);

        WalletBetCreditDto dto = this.prepareOperatorRequestData(walletRequest);

        try {
            new WalletBetCreditAction().callToOperator(walletRequest, dto);

        } finally {
            walletTransaction.setOperatorStatus(walletRequest.getOperatorResponseStatus().code);
            walletTransactionService.save(walletTransaction);

            walletRequest = this.generateSettledBet(walletTransaction, walletRequest);
            walletRequest = this.generateBetHistory(walletTransaction, walletRequest);
            return walletRequest;

        }
    }

    public WalletRequest generateBetHistory(WalletTransaction walletTransaction, WalletRequest walletRequest) throws InternalServerException {

        try {
            BetHistory betHistory = this.prepareBetHistoryEntity(walletTransaction, walletRequest);

            if (!walletRequest.getIsPreProcessBet()) {
                kafkaService.produceBetHistoryV3(betHistory, null, null, null, walletRequest.getOperatorUsername(), walletRequest.getVendorPlayerUsername());
            } else {
                kafkaService.producePreprocessingBetHistory(betHistory, walletRequest.getOperatorUsername(), walletRequest.getVendorPlayerUsername(), walletRequest.getFromVendorRate());
            }

        } catch (Exception e) {
            //TODO REVISIT AND CAPTURE THE FAILED INTERNAL PROCESS TO RERUN IT
            throw new InternalServerException(e.getMessage());

        }
        return walletRequest;

    }

    public WalletBetCreditDto prepareOperatorRequestData(WalletRequest walletRequest) {
        WalletBetCreditDto dto = new WalletBetCreditDto();
        BigDecimal amount = walletRequest.getTransferAmount();
        BigDecimal betAmount = walletRequest.getBetAmount();
        BigDecimal winAmount = walletRequest.getWinAmount();
        BigDecimal effectiveTurnover = walletRequest.getEffectiveTurnover();
        BigDecimal jackpotAmount = walletRequest.getJackpotAmount();
        BigDecimal winLoss = walletRequest.getWinLoss();
        BigDecimal vendorRate = walletRequest.getFromVendorRate();

        dto.setAmount(CurrencyConversionService.convertFromVendorRate(amount, vendorRate, true));
        dto.setBetAmount(CurrencyConversionService.convertFromVendorRate(betAmount, vendorRate, true));
        dto.setWinAmount(CurrencyConversionService.convertFromVendorRate(winAmount, vendorRate, true));
        dto.setWinLoss(CurrencyConversionService.convertFromVendorRate(winLoss, vendorRate, false)); // winloss can be negative, but still require conversion
        dto.setEffectiveTurnover(CurrencyConversionService.convertFromVendorRate(effectiveTurnover, vendorRate, true));
        dto.setJackpotAmount(CurrencyConversionService.convertFromVendorRate(jackpotAmount, vendorRate, true));

        dto.setTraceId(walletRequest.getTraceId());
        dto.setUsername(walletRequest.getOperatorUsername());
        dto.setTransactionId(walletRequest.getTransactionId());
        dto.setBetId((walletRequest.getBetId()));
        dto.setRoundId(walletRequest.getRoundId());
        dto.setGameCode(walletRequest.getGameCode());
        dto.setCurrency(walletRequest.getCurrencyCode());
        dto.setToken(walletRequest.getToken());
        dto.setBetTime(walletRequest.getVendorBetTime());
        dto.setSettledTime(walletRequest.getVendorSettleTime());
        dto.setTimestamp(walletRequest.getTimestamp());
        dto.setIsRefund((walletRequest.getBetStatus().equals(BetStatus.REFUNDED)) ? 1 : 0);

        return dto;
    }

    private BetHistory prepareBetHistoryEntity(WalletTransaction walletTransaction, WalletRequest walletRequest) {

        BetHistory betHistory = new BetHistory();
        betHistory.setId(walletTransaction.getBetId());
        betHistory.setExternalTransactionId(walletTransaction.getExternalTransactionId());
        betHistory.setVendorBetId(walletTransaction.getVendorBetId());
        betHistory.setRoundId(walletTransaction.getRoundId());
        betHistory.setVendorId(walletTransaction.getVendorId());
        betHistory.setCurrencyId(walletTransaction.getCurrencyId());
        betHistory.setGameSessionToken(walletTransaction.getToken());
        betHistory.setOperatorStatus(walletTransaction.getOperatorStatus());

        betHistory.setVendorLineId(walletRequest.getVendorLineId());
        betHistory.setVendorGameId(walletRequest.getVendorGameId());
        betHistory.setVendorPlayerId(walletRequest.getVendorPlayerId());
        betHistory.setAgentPlayerId(walletRequest.getAgentPlayerId());
        betHistory.setAgentId(walletRequest.getAgentId());
        betHistory.setGameCategoryId(walletRequest.getGameCategoryId());
        betHistory.setBetAmount(walletRequest.getBetAmount());
        betHistory.setWinAmount(walletRequest.getWinAmount());

        BigDecimal winLoss = walletRequest.getWinAmount().subtract(walletRequest.getBetAmount());

        betHistory.setWinLoss(winLoss);
        betHistory.setEffectiveTurnover(walletRequest.getEffectiveTurnover());
        betHistory.setJackpotAmount(walletRequest.getJackpotAmount());
        betHistory.setResultType(BetHistory.retrieveResultType(walletRequest.getWinAmount(), walletRequest.getJackpotAmount()));
        betHistory.setBetType(BetType.NORMAL_BET.code);
        betHistory.setIsFreespin(0);
        betHistory.setStatus((walletRequest.getBetStatus() == null) ? BetStatus.SETTLED.code : walletRequest.getBetStatus().code);
        betHistory.setVendorBetTime(walletRequest.getVendorBetTime());
        betHistory.setVendorSettleTime(walletRequest.getVendorSettleTime());
        betHistory.setResultTime(walletRequest.getTimestamp());

        return betHistory;
    }

    public WalletRequest generateSettledBet(WalletTransaction walletTransaction, WalletRequest walletRequest) {
        try {
            SettledBet settledBet = this.prepareSettledBetEntity(walletTransaction, walletRequest);
            settledBetService.save(settledBet, null);

            if (walletRequest.getBetStatus().equals(BetStatus.REFUNDED)) {
                RawBetRefundLog rawBetRefundLog = this.prepareRawBetRefundLogEntity(walletTransaction, walletRequest);
                betRefundLogService.create(rawBetRefundLog);
            } else {
                RawBetResultLog rawBetResultLog = this.prepareRawBetResultLogEntity(walletTransaction, walletRequest);
                betResultLogService.save(rawBetResultLog);
            }
        } catch (Exception e) {
            // set failed message and continue process
            walletRequest.setErrorMessage(e.getMessage());
        }
        return walletRequest;
    }

    private SettledBet prepareSettledBetEntity(WalletTransaction walletTransaction, WalletRequest walletRequest) {
        SettledBet settledBet = new SettledBet();

        settledBet.setId(walletTransaction.getId());
        settledBet.setBetId(walletTransaction.getBetId());
        settledBet.setInternalTransactionId(walletTransaction.getTransactionId());
        settledBet.setExternalTransactionId(walletTransaction.getExternalTransactionId());
        settledBet.setVendorBetId(walletTransaction.getVendorBetId());
        settledBet.setRoundId(walletTransaction.getRoundId());
        settledBet.setVendorGameId(walletRequest.getVendorGameId());
        settledBet.setVendorPlayerId(walletRequest.getVendorPlayerId());
        settledBet.setVendorId(walletTransaction.getVendorId());
        settledBet.setVendorLineId(walletRequest.getVendorLineId());
        settledBet.setAgentPlayerId(walletRequest.getAgentPlayerId());
        settledBet.setAgentId(walletRequest.getAgentId());
        settledBet.setOperatorStatus(walletTransaction.getOperatorStatus());
        settledBet.setCurrencyId(walletTransaction.getCurrencyId());
        settledBet.setBetAmount(walletRequest.getBetAmount());
        settledBet.setWinAmount(walletRequest.getWinAmount());
        settledBet.setJackpotAmount(walletRequest.getJackpotAmount());
        BigDecimal winLoss = walletRequest.getWinAmount().subtract(walletRequest.getBetAmount());
        settledBet.setWinLoss(winLoss);
        settledBet.setEffectiveTurnover(walletRequest.getEffectiveTurnover());

        settledBet.setResultType(BetHistory.retrieveResultType(walletRequest.getWinAmount(), walletRequest.getJackpotAmount()));
        settledBet.setIsFreespin(0);
        settledBet.setRawData(null);
        settledBet.setResettleNum(0);
        settledBet.setStatus((walletRequest.getBetStatus() == null) ? BetStatus.SETTLED.code : walletRequest.getBetStatus().code);
        settledBet.setGameSessionToken(walletTransaction.getToken());
        settledBet.setGameCategoryId(walletRequest.getGameCategoryId());
        settledBet.setVendorBetTime(walletRequest.getVendorBetTime());
        settledBet.setVendorSettleTime(walletRequest.getVendorSettleTime());
        settledBet.setCreateTime(System.currentTimeMillis());
        settledBet.setResultTime(walletRequest.getTimestamp());
        settledBet.setBalance(walletTransaction.getBalance());
        settledBet.setBetType(BetType.NORMAL_BET.code);
        return settledBet;
    }

    private RawBetResultLog prepareRawBetResultLogEntity(WalletTransaction walletTransaction, WalletRequest walletRequest) {
        RawBetResultLog rawBetResultLog = new RawBetResultLog();

        rawBetResultLog.setId(walletTransaction.getId());
        rawBetResultLog.setBetHistoryId(walletTransaction.getBetId());
        rawBetResultLog.setResultLogId(walletRequest.getTraceId());
        rawBetResultLog.setExternalTransactionId(walletTransaction.getExternalTransactionId());
        rawBetResultLog.setRoundId(walletTransaction.getRoundId());
        rawBetResultLog.setVendorGameId(walletRequest.getVendorGameId());
        rawBetResultLog.setVendorPlayerId(walletRequest.getVendorPlayerId());
        rawBetResultLog.setAgentPlayerId(walletRequest.getAgentPlayerId());
        rawBetResultLog.setAgentId(walletRequest.getAgentId());
        rawBetResultLog.setOperatorStatus(walletTransaction.getOperatorStatus());
        rawBetResultLog.setVendorLineId(walletRequest.getVendorLineId());
        rawBetResultLog.setCurrencyId(walletRequest.getCurrencyId());
        rawBetResultLog.setVendorCurrencyCode(walletRequest.getCurrencyCode());
        rawBetResultLog.setWinAmount(walletRequest.getWinAmount());
        rawBetResultLog.setEffectiveTurnover(walletRequest.getEffectiveTurnover());
        rawBetResultLog.setResultType(BetHistory.retrieveResultType(walletRequest.getWinAmount(), walletRequest.getJackpotAmount()));
        rawBetResultLog.setBalance(walletTransaction.getBalance());
        rawBetResultLog.setStatus((walletRequest.getBetStatus() == null) ? BetStatus.SETTLED.code : walletRequest.getBetStatus().code);
        rawBetResultLog.setVendorTime(walletRequest.getVendorSettleTime());
        rawBetResultLog.setCreateTime(System.currentTimeMillis());
        return rawBetResultLog;
    }

    private RawBetRefundLog prepareRawBetRefundLogEntity(WalletTransaction walletTransaction, WalletRequest walletRequest) {
        RawBetRefundLog rawBetRefundLog = new RawBetRefundLog();

        rawBetRefundLog.setId(walletTransaction.getId());
        rawBetRefundLog.setBetRefundLogId(walletRequest.getTraceId());
        rawBetRefundLog.setBetHistoryId(walletTransaction.getBetId());
        rawBetRefundLog.setExternalTransactionId(walletTransaction.getExternalTransactionId());
        rawBetRefundLog.setRoundId(walletTransaction.getRoundId());
        rawBetRefundLog.setVendorGameId(walletRequest.getVendorGameId());
        rawBetRefundLog.setVendorPlayerId(walletRequest.getVendorPlayerId());
        rawBetRefundLog.setVendorLineId(walletRequest.getVendorLineId());
        rawBetRefundLog.setAgentPlayerId(walletRequest.getAgentPlayerId());
        rawBetRefundLog.setAgentId(walletRequest.getAgentId());
        rawBetRefundLog.setOperatorStatus(walletTransaction.getOperatorStatus());
        rawBetRefundLog.setCurrencyId(walletRequest.getCurrencyId());
        rawBetRefundLog.setCreateTime(System.currentTimeMillis());
        rawBetRefundLog.setBalance(walletTransaction.getBalance());
        return rawBetRefundLog;
    }
}
