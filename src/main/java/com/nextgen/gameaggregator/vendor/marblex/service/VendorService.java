package com.nextgen.gameaggregator.vendor.marblex.service;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.marblex.api.cancel.CancelDto;
import com.nextgen.gameaggregator.vendor.marblex.api.refund.RefundDto;
import com.nextgen.gameaggregator.vendor.marblex.constant.StatusCode;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonDataVo;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class VendorService extends BaseVendorService {
    public final VendorLineService vendorLineService;
    public final AgentPlayerService agentPlayerService;
    public final VendorGameService vendorGameService;
    public final ValidationService validationService;
    public final WalletService walletService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public VendorService(VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService,
                         VendorGameService vendorGameService,
                         ValidationService validationService,
                         WalletService walletService,
                         RequestIdempotentLogService requestIdempotentLogService) {
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.validationService = validationService;
        this.walletService = walletService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo mapToSuccess(String currency, BigDecimal balance) {
        return new CommonVo()
                .setStatusCode(StatusCode.SUCCESS)
                .setData(new CommonDataVo()
                        .setBalance(balance)
                        .setCurrency(currency));
    }

    public CommonVo mapIdempotentSuccess(BigDecimal balance, GameSession gameSession, HttpRequestLog httpRequestLog) {
        BigDecimal finalBalance = balance;

        if (finalBalance == null || finalBalance.compareTo(BigDecimal.ZERO) == 0) {
            try {
                finalBalance = walletService.getBalance(gameSession.getTraceId(), gameSession, httpRequestLog);
            } catch (Exception e) {
                finalBalance = BigDecimal.ZERO;
            }
        }

        return new CommonVo()
                .setStatusCode(StatusCode.SUCCESS)
                .setData(new CommonDataVo()
                        .setBalance(finalBalance)
                        .setCurrency(gameSession.getVendorCurrencyCode()));
    }

    public void doVerification(CommonDto dto, GameSession gameSession, boolean checkBet) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, InvalidCurrencyException, AuthenticationException {

        if (checkBet) {
            // validate vendor username, agent vendor line, player status, and game status
            validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
        } else {
            // Verify vendor line is active
            vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

            // Verify agent player is active
            agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

            // Verify vendor game is active
            vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        }

        // Verify player name from dto is equal
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);
    }

    public void doDataMapper(WalletRequest walletRequest, SportBetResultData sportBetResultData) {
        walletRequest.setExternalTransactionId(sportBetResultData.getExternalTransactionId());
        walletRequest.setVendorBetId(sportBetResultData.getVendorBetId());
        walletRequest.setRoundId(sportBetResultData.getRoundId());
        walletRequest.setVendorPlayerUsername(sportBetResultData.getVendorPlayerUsername());
        walletRequest.setBetAmount(sportBetResultData.getBetAmount());
        walletRequest.setNewBetAmount(sportBetResultData.getBetAmount());
        walletRequest.setWinAmount(sportBetResultData.getWinAmount());
        walletRequest.setWinLoss(sportBetResultData.getWinLoss());
        walletRequest.setEffectiveTurnover(sportBetResultData.getBetAmount());
        walletRequest.setVendorBetTime(sportBetResultData.getVendorBetTime());
        walletRequest.setVendorSettleTime(sportBetResultData.getVendorSettleTime());
        walletRequest.setBetType(sportBetResultData.getBetType());
        walletRequest.setBetStatus(sportBetResultData.getBetStatus());
    }

    public void doDataMapper(WalletRequest walletRequest, RefundDto refundDto) {
        walletRequest.setExternalTransactionId(refundDto.getExternalTransactionId());
        walletRequest.setVendorBetId(refundDto.getVendorBetId());
        walletRequest.setRoundId(refundDto.getRoundId());
        walletRequest.setVendorPlayerUsername(refundDto.getVendorPlayerUsername());
    }

    public void doDataMapper(WalletRequest walletRequest, CancelDto cancelDto) {
        if (cancelDto.getExternalTransactionId().equals(walletRequest.getExternalTransactionId())) {
            walletRequest.setExternalTransactionId(cancelDto.getTraceId());
        } else {
            walletRequest.setExternalTransactionId(cancelDto.getExternalTransactionId());
        }
        walletRequest.setVendorBetId(cancelDto.getVendorBetId());
        walletRequest.setRoundId(cancelDto.getRoundId());
        walletRequest.setVendorPlayerUsername(cancelDto.getVendorPlayerUsername());
    }

    public IdempotentState checkIdempotentRequest(String externalTransactionId, String vendorPlayerUsername, String action) throws TransactionStillProcessingException, BetResultIdempotentViolationException {
        RequestIdempotentLog existingLog = requestIdempotentLogService.getSportsRequestIdempotentLog(externalTransactionId, vendorPlayerUsername, action);

        IdempotentState state = new IdempotentState();

        if (existingLog == null) {
            // New request - need to create log later
            state.shouldCreateLog = true;
            return state;
        }

        // Store that we have an existing log
        state.hasExistingLog = true;

        if (existingLog.getOperatorResponseStatus() == ResponseCodes.Status.SC_OK.code) {
            // Check if enough time has passed since creation
//            long currentTime = System.currentTimeMillis();
//            long timeSinceCreation = currentTime - existingLog.getCreateTime();
//
//            // If less than 1 second has passed, still consider it processing
//            if (timeSinceCreation < 1000) {
//                throw new TransactionStillProcessingException();
//            }
//
            // Enough time has passed means this is a true duplicate
            state.shouldSkipCleanup = true;
            throw new BetResultIdempotentViolationException();
        }

        // Request exists but not completed (status != OK) - still processing
        throw new TransactionStillProcessingException();
    }

    public void createIdempotentLogIfNeeded(String externalTransactionId, String vendorPlayerUsername, WalletRequest walletRequest, IdempotentState state, String action) {
        if (state.shouldCreateLog) {
            requestIdempotentLogService.create(externalTransactionId, vendorPlayerUsername, walletRequest.getOperatorResponseStatus().code, action);
        }

        if (walletRequest.getOperatorResponseStatus().code == ResponseCodes.Status.SC_OK.code) {
            // If the request was successful, we don't need to create a log again
            state.shouldSkipCleanup = true; // Don't delete in finally block since we just created with OK status
        }
    }

    public void recreateIdempotentLogWithOkStatus(String externalTransactionId, String vendorPlayerUsername, WalletRequest walletRequest, IdempotentState state, String action) {
        // Only recreate if settlement was successful AND we had an existing log with non-OK status
        if (state.hasExistingLog && walletRequest.getOperatorResponseStatus().code == ResponseCodes.Status.SC_OK.code) {
            // Delete the old log and create new one with OK status
            requestIdempotentLogService.delete(externalTransactionId, vendorPlayerUsername, action);
            requestIdempotentLogService.create(externalTransactionId, vendorPlayerUsername, ResponseCodes.Status.SC_OK.code, action);
            state.shouldSkipCleanup = true; // Don't delete in finally block since we just created with OK status
        }
    }

    public void cleanupIdempotentLog(String externalTransactionId, String vendorPlayerUsername, IdempotentState state, String action) {
        // Only delete if it's a successful new request AND we shouldn't skip cleanup
        if (!state.shouldSkipCleanup) {
            requestIdempotentLogService.delete(externalTransactionId, vendorPlayerUsername, action);
        }
    }

    public static class IdempotentState {
        public boolean shouldCreateLog = false;
        public boolean hasExistingLog = false;
        public boolean shouldSkipCleanup = false;
    }
}
