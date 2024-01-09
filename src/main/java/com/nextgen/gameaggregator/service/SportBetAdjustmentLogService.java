package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.AgentPlayer;
import com.nextgen.gameaggregator.entity.RawBetAdjustmentLog;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.exception.BetAdjustmentIdempotentViolationException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentData;
import com.nextgen.gameaggregator.repository.RawBetAdjustmentLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class SportBetAdjustmentLogService {
    @Autowired
    private RawBetAdjustmentLogRepository rawBetAdjustmentLogRepository;

    public RawBetAdjustmentLog create(RawBetAdjustmentLog entity) {
        return rawBetAdjustmentLogRepository.save(entity);
    }

    public void idempotentCheck(String traceId, String vendorPlayerId, String externalTransactionId) throws BetAdjustmentIdempotentViolationException, TransactionStillProcessingException {
        String id = this.generateId(vendorPlayerId, externalTransactionId);
        RawBetAdjustmentLog rawBetAdjustmentLog = rawBetAdjustmentLogRepository.findById(id).orElse(null);

        if (rawBetAdjustmentLog != null) {
            Integer operatorStatus = rawBetAdjustmentLog.getOperatorStatus();

            if (operatorStatus.equals(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code)) {
                log.warn("idempotentCheckForBetResultLog.processing [" + traceId + "]: transactionId (" + externalTransactionId + ") roundId (" + rawBetAdjustmentLog.getRoundId() + ")");
                throw new TransactionStillProcessingException();

            } else if (operatorStatus.equals(ResponseCodes.Status.SC_OK.code)) {
                log.warn("idempotentCheckForBetResultLog.success [" + traceId + "]: transactionId (" + externalTransactionId + ") roundId (" + rawBetAdjustmentLog.getRoundId() + ")");
                throw new BetAdjustmentIdempotentViolationException(rawBetAdjustmentLog);

            } else {
                rawBetAdjustmentLog.setOperatorStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
                this.create(rawBetAdjustmentLog);
            }
        }
    }

    public RawBetAdjustmentLog newSportBetAdjustmentLog(String traceId, VendorPlayer vendorPlayer, AgentPlayer agentPlayer, SportAdjustmentData sportAdjustmentData, BigDecimal balance) {
        RawBetAdjustmentLog entity = new RawBetAdjustmentLog();
        String vendorPlayerId = vendorPlayer.getId().toString();
        String id = this.generateId(vendorPlayerId, sportAdjustmentData.getExternalTransactionId());

        entity.setId(id);
        entity.setBetAdjustmentId(traceId);
        entity.setBetHistoryId(traceId);
        entity.setExternalTransactionId(sportAdjustmentData.getExternalTransactionId());
        entity.setRoundId(sportAdjustmentData.getRoundId());
        entity.setAmount(sportAdjustmentData.getAmount());
        entity.setVendorLineId(vendorPlayer.getVendorLineId());
        entity.setVendorGameId(999); // Todo get vendor_game_id
        entity.setVendorPlayerId(vendorPlayer.getId());
        entity.setAgentPlayerId(agentPlayer.getId());
        entity.setAgentId(agentPlayer.getAgentId());
        entity.setOperatorStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        entity.setBalance(balance);
        entity.setCurrencyId(vendorPlayer.getCurrencyId());
        entity.setCreateTime(System.currentTimeMillis());

        return entity;
    }

    private String generateId(String vendorPlayerId, String externalTransactionId) {
        String delimiter = "_";
        List<String> list = Arrays.asList(vendorPlayerId, externalTransactionId);

        return String.join(delimiter, list);
    }
}
