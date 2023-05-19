package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.UnsettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.CouchbaseDataIntegrityException;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.RawSettledBetRepository;
import com.nextgen.gameaggregator.entity.SettledBet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class SettledBetService {

    @Autowired
    RawSettledBetRepository rawSettledBetRepository;
    @Autowired
    BetHistoryRepository betHistoryRepository;
    @Autowired
    private BetHistoryService betHistoryService;

    @CachePut(value = "SettledBet", key = "{#settledBet.externalTransactionId, #settledBet.vendorPlayerId}", cacheManager = "cacheManager")
    public SettledBet create(SettledBet settledBet, String rawData) {
        settledBet.setResettleNum(Optional.ofNullable(settledBet.getResettleNum()).orElse(0));
        settledBet.setRawData(Optional.ofNullable(settledBet.getRawData()).orElse(DigestUtils.md5Hex(rawData)));
        settledBet.setCreateTime(System.currentTimeMillis());
        settledBet.setProcessingStatus(0);
        rawSettledBetRepository.save(settledBet);
        return settledBet;
    }

    @Cacheable(value = "SettledBet", key = "{#externalTransactionId, #vendorPlayerId}", cacheManager = "cacheManager")
    public SettledBet getByVendorPlayerIdAndExternalTransactionId(Long vendorPlayerId, String externalTransactionId) throws BetNotFoundException {
        SettledBet settledBet = rawSettledBetRepository.findByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);
        if (settledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find vendor player Id: " + vendorPlayerId + ", externalTransactionId: " + externalTransactionId);
        }

        return settledBet;
    }

    /**
     * Delete settled bet record of the given settleBet entity object after successful inserted into kafka.
     * @param settledBet entity object containing information of a single settled bet
     */
    public void delete(SettledBet settledBet){
        try {
            rawSettledBetRepository.delete(settledBet);
        } catch (Exception e){
            log.warn("Couchbase Delete SettledBet.exception -> vendorBetId = " + settledBet.getVendorBetId() + "& roundId = " + settledBet.getRoundId());
        }
    }

    /**
     * Create a Settled bet record of the given RawSettledBet entity object.
     * This function will also populate default values of certain fields.
     *
     * @param entity RawSettledBet entity object containing information of a single settled bet
     * @return RawSettledBet entity object after a successful save
     */
    @CachePut(value = "SettledBet", key = "{#entity.vendorBetId, #entity.roundId, #entity.vendorGameId, #entity.vendorPlayerId}", cacheManager = "cacheManager")
    public SettledBet createSettledBet(SettledBet entity) throws CouchbaseDataIntegrityException {
        // Set default values
        entity.setStatus(2); // TODO: refactor, map to constant/enum value
        entity.setCreateTime(System.currentTimeMillis());

        try {
            rawSettledBetRepository.save(entity);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {

            throw new CouchbaseDataIntegrityException("Data incorrect : " + dataIntegrityViolationException.getMessage());
        }

        return entity;
    }

    /**
     * Create a Settled bet record of the given RawSettledBet entity object to MariaDB.
     * This function will also populate default values of certain fields.
     *
     * @param entity RawSettledBet entity object containing information of a single settled bet
     */
//    public void createSettleBetMariaDB(SettledBet entity) throws MergedBetDataIntegrityException, CouchbaseDataIntegrityException {
//
//        try {
//            BetHistory betHistory = new BetHistory();
//            BeanUtils.copyProperties(betHistory, entity);
//            betHistory.setRawData(entity.getMd5RawSettledResult());
//            //TODO REMOVING OPERATORSTATUS
//            betHistory.setOperatorStatus(1);
//            betHistory.setId(entity.getInternalTransactionId());
//
//            betHistoryRepository.save(betHistory);
//
//        } catch (IllegalAccessException illegalAccessException) {
//            throw new MergedBetDataIntegrityException("copyProperties invalid : " + illegalAccessException.getMessage());
//        } catch (InvocationTargetException invocationTargetException) {
//            throw new MergedBetDataIntegrityException("copyProperties invalid : " + invocationTargetException.getMessage());
//        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
//            throw new CouchbaseDataIntegrityException("Data incorrect : " + dataIntegrityViolationException.getMessage());
//        }
//    }
}
