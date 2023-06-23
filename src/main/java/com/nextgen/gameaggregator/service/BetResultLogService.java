package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.RawBetResultLog;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.repository.BetResultLogRepository;
import com.nextgen.gameaggregator.repository.RawBetResultLogRepository;
import com.nextgen.gameaggregator.repository.RawResultBetRepository;
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
    private RawResultBetRepository rawResultBetRepository;
    @Autowired
    private RawBetResultLogRepository rawBetResultLogRepository;

    /**
     * Creates a database record of the given BetResultLog entity object.
     * This function will also populate default values of certain fields.
     * Every time a Bet Result is received, a new record will be created.
     *
     * @param entity BetResultLog entity object containing the result of a previous bet
     * @return BetResultLog entity object after a successful save
     */
    public BetResultLog create(BetResultLog entity) {
        // Set default values
        entity.setStatus(1); // TODO: refactor, map to constant/enum value
        entity.setCreateTime(System.currentTimeMillis());

        return betResultLogRepository.save(entity);
    }

    public void update(RawBetResultLog rawBetResultLog) {
        rawBetResultLogRepository.save(rawBetResultLog);
    }

    @CachePut(value = "rawResultLog", key = "{#betResultData.externalTransactionId, #betResultData.roundId, #gameSession.vendorGameId, #gameSession.vendorPlayerId}", cacheManager = "cacheManager")
    public RawBetResultLog create(String traceId, String betId, BetResultData betResultData, GameSession gameSession, BigDecimal balance, Integer operatorStatus) {
        RawBetResultLog entity = this.newRawBetResultLog(traceId, betId, betResultData, gameSession, balance, operatorStatus);
        rawBetResultLogRepository.save(entity);
        return entity;
    }

    @Cacheable(value = "rawResultLog", key = "{#transactionId, #roundId, #vendorGameId, #vendorPlayerId}", cacheManager = "cacheManager")
    public RawBetResultLog checkExists(String transactionId, String roundId, String vendorGameId, String vendorPlayerId) {
        String id = this.generateId(transactionId, roundId, vendorGameId, vendorPlayerId);

        return rawBetResultLogRepository.findById(id).orElse(null);
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
