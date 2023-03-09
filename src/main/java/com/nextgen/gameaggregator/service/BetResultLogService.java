package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.entity.RawResultBet;
import com.nextgen.gameaggregator.exception.CouchbaseDataIntegrityException;
import com.nextgen.gameaggregator.repository.BetResultLogRepository;
import com.nextgen.gameaggregator.repository.RawResultBetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BetResultLogService {
    @Autowired
    private BetResultLogRepository betResultLogRepository;
    @Autowired
    private RawResultBetRepository rawResultBetRepository;

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
    @CachePut(value = "ResultBet", key = "{#entity.roundId, #entity.vendorGameId, #entity.vendorPlayerId}", cacheManager = "cacheManager")
    public RawResultBet createResultBet(RawResultBet entity) throws CouchbaseDataIntegrityException {
        // Set default values
        entity.setStatus(1); // TODO: refactor, map to constant/enum value
        entity.setCreateTime(System.currentTimeMillis());

        try{
            rawResultBetRepository.save(entity);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {

            throw new CouchbaseDataIntegrityException("Data incorrect : " + dataIntegrityViolationException.getMessage());
        }

        return entity;
    }
}
