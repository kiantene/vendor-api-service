package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.CouchbaseDataIntegrityException;
import com.nextgen.gameaggregator.exception.MergedBetDataIntegrityException;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.RawSettledBetRepository;
import org.apache.commons.beanutils.BeanUtils;
import com.nextgen.gameaggregator.entity.UnsettledBetResult;
import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

    public void create(SettledBet settledBet, String rawData) {
        settledBet.setResettleNum(Optional.ofNullable(settledBet.getResettleNum()).orElse(0));
        settledBet.setRawData(Optional.ofNullable(settledBet.getRawData()).orElse(DigestUtils.md5Hex(rawData)));
        rawSettledBetRepository.save(settledBet);
    }

    public SettledBet getByVendorIdAndExternalTransactionId(Integer vendorId, String externalTransactionId) throws BetNotFoundException {
        SettledBet settledBet = rawSettledBetRepository.findByVendorIdAndExternalTransactionId(vendorId, externalTransactionId);
        if (settledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find vendor Id: " + vendorId + ", externalTransactionId: " + externalTransactionId);
        }

        return settledBet;
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
