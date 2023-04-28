package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.RawBetResultLog;
import com.nextgen.gameaggregator.entity.UnsettledBetResult;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.CouchbaseDataIntegrityException;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.repository.BetResultLogRepository;
import com.nextgen.gameaggregator.repository.RawBetResultLogRepository;
import com.nextgen.gameaggregator.repository.RawResultBetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * Creates a Result bet record of the given RawResultBet entity object.
     * This function will also populate default values of certain fields.
     *
     * @param entity RawResultBet entity object containing information of a single result bet
     * @return RawResultBet entity object after a successful save
     */
    @CachePut(value = "BetResult", key = "{#entity.vendorBetId, #entity.roundId, #entity.vendorGameId, #entity.vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBetResult create(UnsettledBetResult entity) throws CouchbaseDataIntegrityException {
        // Set default values
        entity.setStatus(BetStatus.UNSETTLED.code);
        entity.setCreateTime(System.currentTimeMillis());

        // TODO: check if couchbase will throw dataIntegrityViolationException
        try {
            rawResultBetRepository.save(entity);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {

            throw new CouchbaseDataIntegrityException("Data incorrect : " + dataIntegrityViolationException.getMessage());
        }

        return entity;
    }

    /**
     * Retrieve a result bet transaction record based on vendor's round Id, game Id, and player Id
     *
     * @param roundId        Vendor's round Id
     * @param vendorGameId   vendor line id within Game Aggregator System
     * @param vendorPlayerId Id of the record in VendorPlayer
     * @return result bet entity object containing all information of a single result Bet
     * If no bet record is found, return null (valid scenario)
     */
    @Cacheable(value = "ResultBet", key = "{#vendorBetId, #roundId, #vendorGameId, #vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBetResult getRawResultBetByRoundId(String vendorBetId, String roundId, Integer vendorGameId, Long vendorPlayerId) {

        String mergeId = vendorBetId + '_' + roundId + '_' + vendorGameId + '_' + vendorPlayerId;
        return rawResultBetRepository.findById(mergeId).orElse(null);
    }

    public RawBetResultLog create(String traceId, String betId, BetResultData betResultData, GameSession gameSession, BigDecimal balance) {
        RawBetResultLog entity = this.newRawBetResultLog(traceId, betId, betResultData, gameSession, balance);
        rawBetResultLogRepository.save(entity);
        return entity;
    }

    public RawBetResultLog checkExists(String transactionId, String roundId, String vendorGameId, String vendorPlayerId) {
        String delimiter = "_";
        List<String> list = Arrays.asList(transactionId, roundId, vendorGameId, vendorPlayerId);
        String id = String.join(delimiter, list);

        return rawBetResultLogRepository.findById(id).orElse(null);
    }

    private RawBetResultLog newRawBetResultLog(String traceId, String betId, BetResultData betResultData, GameSession gameSession, BigDecimal balance) {
        RawBetResultLog entity = new RawBetResultLog();

        entity.setId(traceId);
        entity.setBetHistoryId(betId);
        entity.setExternalTransactionId(betResultData.getExternalTransactionId());
        entity.setRoundId(betResultData.getRoundId());
        entity.setVendorGameId(gameSession.getVendorGameId());
        entity.setVendorPlayerId(gameSession.getVendorPlayerId());
        entity.setAgentPlayerId(gameSession.getAgentPlayerId());
        entity.setAgentId(gameSession.getAgentId());
        entity.setOperatorStatus(0);
        entity.setVendorLineId(gameSession.getVendorLineId());
        entity.setCurrencyId(gameSession.getCurrencyId());
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
