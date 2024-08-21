package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.BetRefundLog;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.RawBetRefundLog;
import com.nextgen.gameaggregator.exception.BetRefundIdempotentViolationException;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.repository.ga.writer.BetRefundLogRepository;
import com.nextgen.gameaggregator.repository.ga.writer.RawBetRefundLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class BetRefundLogService {
    @Autowired
    private BetRefundLogRepository betRefundLogRepository;
    @Autowired
    private RawBetRefundLogRepository rawBetRefundLogRepository;

    /**
     * Creates a database record of the given BetRefundLog entity object.
     * This function will also populate default values of certain fields.
     * Every time a Bet Refund is received, a new record will be created.
     *
     * @param entity BetRefundLog entity object containing vendor's unique transaction Id to reverse the bet
     * @return BetRefundLog entity object after a successful save
     */
    public BetRefundLog create(BetRefundLog entity) {
        // Set default values
        entity.setCreateTime(System.currentTimeMillis());

        return betRefundLogRepository.save(entity);
    }

    public RawBetRefundLog create(RawBetRefundLog entity) {
        return rawBetRefundLogRepository.save(entity);
    }

    private String generateId(String vendorPlayerId, String vendorGameId, String externalTransactionId) {
        String delimiter = "_";
        List<String> list = Arrays.asList(vendorPlayerId, vendorGameId, externalTransactionId);

        return String.join(delimiter, list);
    }

    public void idempotentCheck(Long vendorPlayerId, Integer vendorGameId, String externalTransactionId) throws BetRefundIdempotentViolationException {
        RawBetRefundLog rawBetRefundLog = this.checkExists(vendorPlayerId.toString(), vendorGameId.toString(), externalTransactionId);

        if (rawBetRefundLog != null) {
            BetRefundIdempotentViolationException idempotentViolationException = new BetRefundIdempotentViolationException();
            idempotentViolationException.setBetRefundLog(rawBetRefundLog);
            throw idempotentViolationException;
        }
    }

    @Cacheable(value = "rawBetRefundLogs", key = "{#vendorPlayerId, #roundId}", cacheManager = "cacheManager", unless = "#result == null")
    public RawBetRefundLog checkExistsByRoundId(Long vendorPlayerId, String roundId) {
        return rawBetRefundLogRepository.findByVendorPlayerIdAndRoundId(vendorPlayerId, roundId);
    }

    public RawBetRefundLog checkExists(String vendorPlayerId, String vendorGameId, String externalTransactionId) {
        String id = this.generateId(vendorPlayerId, vendorGameId, externalTransactionId);

        return rawBetRefundLogRepository.findById(id).orElse(null);
    }

    public RawBetRefundLog newRawBetRefundLog(String traceId, String betId, RollbackData rollbackData, String roundId, GameSession gameSession, BigDecimal balance) {
        RawBetRefundLog entity = new RawBetRefundLog();
        String vendorPlayerId = gameSession.getVendorPlayerId().toString();
        String vendorGameId = gameSession.getVendorGameId().toString();
        String externalTransactionId = rollbackData.getRollbackId();
        String id = this.generateId(vendorPlayerId, vendorGameId, externalTransactionId);

        entity.setId(id);
        entity.setBetHistoryId(betId);
        entity.setBetRefundLogId(traceId);
        entity.setExternalTransactionId(externalTransactionId);
        entity.setRoundId(roundId);
        entity.setVendorGameId(gameSession.getVendorGameId());
        entity.setVendorPlayerId(gameSession.getVendorPlayerId());
        entity.setAgentPlayerId(gameSession.getAgentPlayerId());
        entity.setAgentId(gameSession.getAgentId());
        entity.setOperatorStatus(1);
        entity.setVendorLineId(gameSession.getVendorLineId());
        entity.setCurrencyId(gameSession.getCurrencyId());
        entity.setCreateTime(System.currentTimeMillis());
        entity.setBalance(balance);

        return entity;
    }
}
