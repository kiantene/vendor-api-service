package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.exception.CouchbaseDataIntegrityException;
import com.nextgen.gameaggregator.exception.MergedBetDataIntegrityException;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.RawSettledBetRepository;
import org.apache.commons.beanutils.BeanUtils;
import com.nextgen.gameaggregator.entity.RawResultBet;
import com.nextgen.gameaggregator.entity.RawSettledBet;
import com.nextgen.gameaggregator.entity.RawUnsettledBet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

@Service
@Slf4j
public class SettledBetService {

    @Autowired
    RawSettledBetRepository rawSettledBetRepository;
    @Autowired
    BetHistoryRepository betHistoryRepository;

    /**
     * Creates a Result bet record of the given RawResultBet entity object.
     * This function will also populate default values of certain fields.
     *
     * @param  rawUnsettledBet, rawResultBet, rawSettledBet entity object containing information of a single result bet
     * @return RawResultBet entity object after a successful save
     */
    public RawSettledBet updateRawSettledBet(RawUnsettledBet rawUnsettledBet, RawResultBet rawResultBet, RawSettledBet rawSettledBet)
            throws MergedBetDataIntegrityException {

        try {
            RawSettledBet unsettledData = new RawSettledBet();
            BeanUtils.copyProperties(unsettledData, rawUnsettledBet);

            //resultData could be null if the bet is lose
            RawSettledBet resultData = new RawSettledBet();
            if(rawResultBet != null){
                BeanUtils.copyProperties(resultData, rawResultBet);
            }

            for (Field field : RawSettledBet.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = getValueFromObject(rawSettledBet, field.getName());
                if (value == null) {
                    value = getValueFromObject(resultData, field.getName());
                }
                if (value == null) {
                    value = getValueFromObject(unsettledData, field.getName());
                }
                if (value != null) {
                    field.set(rawSettledBet, value);
                }
            }

        } catch (IllegalAccessException illegalAccessException) {
            throw new MergedBetDataIntegrityException("getValueFromObject invalid : " + illegalAccessException.getMessage());

        } catch (InvocationTargetException invocationTargetException) {
            throw new MergedBetDataIntegrityException("copyProperties invalid : " + invocationTargetException.getMessage());
        }

        return rawSettledBet;
    }



    /**
     * Get values that is not null from the object.
     *
     * @param  object, fieldName, entity object containing information of a single bet
     * @return Object entity after getting all the non-null properties
     */
    private Object getValueFromObject(RawSettledBet object, String fieldName) throws IllegalAccessException {
        Field field;
        try {
            field = object.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
        field.setAccessible(true);
        return field.get(object);
    }

    /**
     * Create a Settled bet record of the given RawSettledBet entity object.
     * This function will also populate default values of certain fields.
     *
     * @param entity RawSettledBet entity object containing information of a single settled bet
     * @return RawSettledBet entity object after a successful save
     */
    @CachePut(value = "SettledBet", key = "{#entity.roundId, #entity.vendorGameId, #entity.vendorPlayerId}", cacheManager = "cacheManager")
    public RawSettledBet createSettledBet(RawSettledBet entity) throws CouchbaseDataIntegrityException {
        // Set default values
        entity.setStatus(2); // TODO: refactor, map to constant/enum value
        entity.setCreateTime(System.currentTimeMillis());

        try{
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
    public void createSettleBetMariaDB(RawSettledBet entity) throws MergedBetDataIntegrityException, CouchbaseDataIntegrityException {

        try {
            BetHistory betHistory = new BetHistory();
            BeanUtils.copyProperties(betHistory, entity);
            betHistory.setRawData(entity.getMd5RawSettledResult());
            //TODO HOUSE AND MASTERAGENT ID MAPPING
            betHistory.setHouseId(0);
            betHistory.setMasterAgentId(0);
            //TODO REMOVING OPERATORSTATUS
            betHistory.setOperatorStatus(1);

            betHistoryRepository.save(betHistory);

        } catch (IllegalAccessException illegalAccessException) {
            throw new MergedBetDataIntegrityException("copyProperties invalid : " + illegalAccessException.getMessage());
        } catch (InvocationTargetException invocationTargetException) {
            throw new MergedBetDataIntegrityException("copyProperties invalid : " + invocationTargetException.getMessage());
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw new CouchbaseDataIntegrityException("Data incorrect : " + dataIntegrityViolationException.getMessage());
        }
    }
}
