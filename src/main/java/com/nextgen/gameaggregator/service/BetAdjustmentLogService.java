package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.RawBetAdjustmentLog;
import com.nextgen.gameaggregator.exception.BetAdjustmentIdempotentViolationException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.adjustment.AdjustmentData;
import com.nextgen.gameaggregator.repository.RawBetAdjustmentLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class BetAdjustmentLogService {

    @Autowired
    private RawBetAdjustmentLogRepository rawBetAdjustmentLogRepository;
    @Autowired
    private BetIdempotentLogService betIdempotentLogService;

    public RawBetAdjustmentLog create(RawBetAdjustmentLog entity) {
        return rawBetAdjustmentLogRepository.save(entity);
    }

    public RawBetAdjustmentLog idempotentCheck(String traceId, GameSession gameSession, AdjustmentData adjustmentData) throws BetAdjustmentIdempotentViolationException, TransactionStillProcessingException {
        RawBetAdjustmentLog rawBetAdjustmentLog = this.checkExists(gameSession.getVendorPlayerId().toString(), gameSession.getVendorGameId().toString(), adjustmentData.getExternalTransactionId());

        if (rawBetAdjustmentLog != null) {
            Integer operatorStatus = rawBetAdjustmentLog.getOperatorStatus();
            Long betTimingDifferenceInMillieSeconds = betIdempotentLogService.compareWithExistingTimingDifference(rawBetAdjustmentLog.getCreateTime());

            if (operatorStatus.equals(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code) && betTimingDifferenceInMillieSeconds < betIdempotentLogService.getTimingDifferenceForStillProcessing()) {
                throw new TransactionStillProcessingException();

            } else if (operatorStatus.equals(ResponseCodes.Status.SC_OK.code)) {
                throw new BetAdjustmentIdempotentViolationException(rawBetAdjustmentLog);

            } else {
                rawBetAdjustmentLog.setOperatorStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
                this.create(rawBetAdjustmentLog);
            }
        } else {
            rawBetAdjustmentLog = this.newRawBetAdjustmentLog(traceId, adjustmentData, gameSession, BigDecimal.ZERO);
            this.create(rawBetAdjustmentLog);
        }

        return rawBetAdjustmentLog;
    }

    public RawBetAdjustmentLog checkExists(String vendorPlayerId, String vendorGameId, String externalTransactionId) {
        String id = this.generateId(vendorPlayerId, vendorGameId, externalTransactionId);

        return rawBetAdjustmentLogRepository.findById(id).orElse(null);
    }

    public RawBetAdjustmentLog newRawBetAdjustmentLog(String traceId, AdjustmentData adjustmentData, GameSession gameSession, BigDecimal balance) {
        RawBetAdjustmentLog entity = new RawBetAdjustmentLog();
        String vendorPlayerId = gameSession.getVendorPlayerId().toString();
        String vendorGameId = gameSession.getVendorGameId().toString();
        String id = this.generateId(vendorPlayerId, vendorGameId, adjustmentData.getExternalTransactionId());

        entity.setId(id);
        entity.setBetAdjustmentId(traceId);
        entity.setBetHistoryId(traceId);
        entity.setExternalTransactionId(adjustmentData.getExternalTransactionId());
        entity.setRoundId(adjustmentData.getRoundId());
        entity.setAmount(adjustmentData.getAdjustmentAmount());
        entity.setVendorLineId(gameSession.getVendorLineId());
        entity.setVendorGameId(gameSession.getVendorGameId());
        entity.setVendorPlayerId(gameSession.getVendorPlayerId());
        entity.setAgentPlayerId(gameSession.getAgentPlayerId());
        entity.setAgentId(gameSession.getAgentId());
        entity.setOperatorStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        entity.setBalance(balance);
        entity.setCurrencyId(gameSession.getCurrencyId());
        entity.setCreateTime(System.currentTimeMillis());

        return entity;
    }

    private String generateId(String vendorPlayerId, String vendorGameId, String externalTransactionId) {
        String delimiter = "_";
        List<String> list = Arrays.asList(vendorPlayerId, vendorGameId, externalTransactionId);

        return String.join(delimiter, list);
    }
}
