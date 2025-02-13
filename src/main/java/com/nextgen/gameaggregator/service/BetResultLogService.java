package com.nextgen.gameaggregator.service;

import com.couchbase.client.core.error.AmbiguousTimeoutException;
import com.couchbase.client.core.error.UnambiguousTimeoutException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.RawBetResultLog;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.repository.ga.writer.BetResultLogRepository;
import com.nextgen.gameaggregator.repository.ga.writer.RawBetResultLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class BetResultLogService {
    @Autowired
    private BetResultLogRepository betResultLogRepository;
    @Autowired
    private RawBetResultLogRepository rawBetResultLogRepository;
    @Autowired
    private BetIdempotentLogService betIdempotentLogService;

    @CachePut(value = "rawResultLog", key = "{#rawBetResultLog.externalTransactionId, #rawBetResultLog.roundId, #rawBetResultLog.vendorGameId, #rawBetResultLog.vendorPlayerId}", cacheManager = "cacheManager")
    public void save(RawBetResultLog rawBetResultLog) {
        rawBetResultLogRepository.save(rawBetResultLog);
    }

    @CachePut(value = "rawResultLog", key = "{#betResultData.externalTransactionId, #betResultData.roundId, #gameSession.vendorGameId, #gameSession.vendorPlayerId}", cacheManager = "cacheManager")
    public RawBetResultLog create(String traceId, String betId, BetResultData betResultData, GameSession gameSession, BigDecimal balance, Integer operatorStatus) {
        RawBetResultLog entity = this.newRawBetResultLog(traceId, betId, betResultData, gameSession, balance, operatorStatus);
        rawBetResultLogRepository.save(entity);
        return entity;
    }

    @Cacheable(value = "rawResultLog", key = "{#transactionId, #roundId, #vendorGameId, #vendorPlayerId}", cacheManager = "cacheManager")
    public RawBetResultLog checkExists(String externalTransactionId, String roundId, String vendorGameId, String vendorPlayerId) {
        String id = this.generateId(externalTransactionId, roundId, vendorGameId, vendorPlayerId);

        return rawBetResultLogRepository.findById(id).orElse(null);
    }

    public RawBetResultLog idempotentCheck(String traceId, GameSession gameSession, BetResultData betResultData)
            throws TransactionStillProcessingException, BetResultIdempotentViolationException,
            AmbiguousTimeoutException, UnambiguousTimeoutException {

        String externalTransactionId = betResultData.getExternalTransactionId();
        String roundId = betResultData.getRoundId();
        String vendorGameId = gameSession.getVendorGameId().toString();
        String vendorPlayerId = gameSession.getVendorPlayerId().toString();
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;

        RawBetResultLog rawBetResultLog = this.checkExists(externalTransactionId, roundId, vendorGameId, vendorPlayerId);

        if (rawBetResultLog != null) {
            Integer operatorStatus = rawBetResultLog.getOperatorStatus();
            Long betTimingDifferenceInMillieSeconds = betIdempotentLogService.compareWithExistingTimingDifference(rawBetResultLog.getCreateTime());

            // throw idempotent exception if status is processing or success
            if (operatorStatus.equals(operatorStatusProcessing) && betTimingDifferenceInMillieSeconds < betIdempotentLogService.getTimingDifferenceForStillProcessing()) {
                throw new TransactionStillProcessingException();

            } else if (operatorStatus.equals(operatorStatusSuccess)) {
                throw new BetResultIdempotentViolationException(rawBetResultLog);

            } else { // when bet result found and operator status is error, set status back to processing and resend txn to operator
                rawBetResultLog.setOperatorStatus(operatorStatusProcessing);
                this.save(rawBetResultLog);
            }
        } else {
            // create bet result log first with betId = 0
            rawBetResultLog = this.create(traceId, "0", betResultData, gameSession, BigDecimal.ZERO, operatorStatusProcessing);
        }

        return rawBetResultLog;
    }

    private String generateId(String transactionId, String roundId, String vendorGameId, String vendorPlayerId) {
        String delimiter = "_";
        List<String> list = Arrays.asList(transactionId, roundId, vendorGameId, vendorPlayerId);

        return String.join(delimiter, list);
    }

    private RawBetResultLog newRawBetResultLog(String traceId, String betId, BetResultData betResultData, GameSession gameSession, BigDecimal balance, Integer operatorStatus) {
        RawBetResultLog entity = new RawBetResultLog();
        String vendorGameId = gameSession.getVendorGameId().toString();
        String vendorPlayerId = gameSession.getVendorPlayerId().toString();
        String id = this.generateId(betResultData.getExternalTransactionId(), betResultData.getRoundId(), vendorGameId, vendorPlayerId);

        entity.setId(id);
        entity.setBetHistoryId(betId);
        entity.setResultLogId(traceId);
        entity.setExternalTransactionId(betResultData.getExternalTransactionId());
        entity.setRoundId(betResultData.getRoundId());
        entity.setVendorGameId(gameSession.getVendorGameId());
        entity.setVendorPlayerId(gameSession.getVendorPlayerId());
        entity.setAgentPlayerId(gameSession.getAgentPlayerId());
        entity.setAgentId(gameSession.getAgentId());
        entity.setOperatorStatus(operatorStatus);
        entity.setVendorLineId(gameSession.getVendorLineId());
        entity.setCurrencyId(gameSession.getCurrencyId());
        entity.setVendorCurrencyCode(gameSession.getVendorCurrencyCode());
        entity.setWinAmount(betResultData.getWinAmount());
        entity.setEffectiveTurnover(BigDecimal.ZERO); // TODO: update accordingly
        entity.setBalance(balance);
        entity.setResultType(1); // TODO: change to enum
        entity.setStatus(1); // TODO: change to enum
        entity.setVendorTime(betResultData.getVendorSettleTime());
        entity.setCreateTime(System.currentTimeMillis());

        return entity;
    }
}
