package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.CouchbaseDataIntegrityException;
import com.nextgen.gameaggregator.exception.MergedBetDataIntegrityException;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.RawSettledBetRepository;
import org.apache.commons.beanutils.BeanUtils;
import com.nextgen.gameaggregator.entity.UnsettledBetResult;
import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import lombok.extern.slf4j.Slf4j;
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

@Service
@Slf4j
public class SettledBetService {

    @Autowired
    RawSettledBetRepository rawSettledBetRepository;
    @Autowired
    BetHistoryRepository betHistoryRepository;
    @Autowired
    private BetHistoryService betHistoryService;

    public void create(SettledBet settledBet) {
        rawSettledBetRepository.save(settledBet);
    }

    /**
     * Creates a Result bet record of the given RawResultBet entity object.
     * This function will also populate default values of certain fields.
     *
     * @param unsettledBet, rawResultBet, rawSettledBet entity object containing information of a single result bet
     * @return RawResultBet entity object after a successful save
     */
    public SettledBet updateRawSettledBet(UnsettledBet unsettledBet, UnsettledBetResult unsettledBetResult, SettledBet settledBet)
            throws MergedBetDataIntegrityException {

        try {
            SettledBet unsettledData = new SettledBet();
            BeanUtils.copyProperties(unsettledData, unsettledBet);

            //resultData could be null if the bet is lose
            SettledBet resultData = new SettledBet();
            if (unsettledBetResult != null) {
                BeanUtils.copyProperties(resultData, unsettledBetResult);
            }

            for (Field field : SettledBet.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = getValueFromObject(settledBet, field.getName());
                if (value == null) {
                    value = getValueFromObject(resultData, field.getName());
                }
                if (value == null) {
                    value = getValueFromObject(unsettledData, field.getName());
                }
                if (value != null) {
                    field.set(settledBet, value);
                }
            }

        } catch (IllegalAccessException illegalAccessException) {
            throw new MergedBetDataIntegrityException("getValueFromObject invalid : " + illegalAccessException.getMessage());

        } catch (InvocationTargetException invocationTargetException) {
            throw new MergedBetDataIntegrityException("copyProperties invalid : " + invocationTargetException.getMessage());
        }

        return settledBet;
    }

    /**
     * Process .
     * This function will also populate default values of certain fields.
     *
     * @param settledBet, entity object containing information of a single result bet
     * @return RawResultBet entity object after a successful save
     */
    public List<SettledBet> getBetResultListData(SettledBet settledBet) {

        //prepare RawSettledBet arrayList
        List<SettledBet> settledBetLists = new ArrayList<>();

        //insert the rawSettledBet into arrayList
        settledBetLists.add(settledBet);

        //try to find are there any unsettled bet with the same roundId + vendorLineId + vendorPlayerId?
        List<UnsettledBet> unsettledBetLists = betHistoryService.getBetDataListByRoundId(settledBet.getRoundId(),
                settledBet.getVendorLineId(), settledBet.getVendorPlayerId());

        try {
            //if found any unsettled bet for with this roundId
            if (unsettledBetLists != null) {
                //then loop thru all unsettled bet list to update the status to settled then add to the rawSettledBet arrayList
                for (UnsettledBet unsettledBetList : unsettledBetLists) {
                    unsettledBetList.setStatus(BetStatus.SETTLED.code);

                    SettledBet settledBetData = new SettledBet();
                    BeanUtils.copyProperties(settledBetData, unsettledBetList);

                    settledBetLists.add(settledBetData);
                }
            }
        } catch (InvocationTargetException e) {
            //TODO ERROR HANDLING IF CONVERSION BETWEEN UNSETTLEDBET TO SETTLEDBET IS FAILED
            log.error("getBetResultListData InvocationTargetException ERROR, details : " + e);
        } catch (IllegalAccessException e) {
            //TODO ERROR HANDLING IF CONVERSION BETWEEN UNSETTLEDBET TO SETTLEDBET IS FAILED
            log.error("getBetResultListData IllegalAccessException ERROR, details : " + e);
        }

        return settledBetLists;

    }

    /**
     * Creates a Result bet record of the given RawResultBet entity object.
     * This function will also populate default values of certain fields.
     *
     * @param unsettledBet, rawResultBet, rawSettledBet entity object containing information of a single result bet
     * @return RawResultBet entity object after a successful save
     */
    public SettledBet updateRawResultBet(UnsettledBet unsettledBet, UnsettledBetResult unsettledBetResult)
            throws MergedBetDataIntegrityException {

        try {
            SettledBet unsettledData = new SettledBet();
            BeanUtils.copyProperties(unsettledData, unsettledBet);

            //resultData could be null if the bet is lose
            SettledBet resultData = new SettledBet();
            BeanUtils.copyProperties(resultData, unsettledBetResult);

            for (Field field : SettledBet.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = getValueFromObject(resultData, field.getName());
                if (value == null) {
                    value = getValueFromObject(unsettledData, field.getName());
                }
                if (value != null) {
                    field.set(resultData, value);
                }
            }

            return resultData;

        } catch (IllegalAccessException illegalAccessException) {
            throw new MergedBetDataIntegrityException("getValueFromObject invalid : " + illegalAccessException.getMessage());

        } catch (InvocationTargetException invocationTargetException) {
            throw new MergedBetDataIntegrityException("copyProperties invalid : " + invocationTargetException.getMessage());
        }
    }

    /**
     * Reprocess betData if winAmount, winLoss, effectiveTurnover, vendorSettleTime, and resultTime is not returned from vendor
     *
     * @param rawBetData entity object containing information of a single result bet
     * @return RawResultBet entity object after a successful recalculation
     */
    public SettledBet processBetData(SettledBet rawBetData) {

        //winLoss will be re-process if empty return from vendor
        if (ObjectUtils.isEmpty(rawBetData.getWinAmount())) {
            //winLoss = 0
            rawBetData.setWinAmount(BigDecimal.valueOf(0));
        }

        //winLoss will be re-process if empty return from vendor
        if (ObjectUtils.isEmpty(rawBetData.getWinLoss())) {
            //winLoss = winAmount - betAmount
            rawBetData.setWinLoss(rawBetData.getWinAmount().subtract(rawBetData.getBetAmount()));
        }

        //effectiveTurnover will be re-process if empty return from vendor
        if (ObjectUtils.isEmpty(rawBetData.getEffectiveTurnover())) {
            //effectiveTurnover = betAmount
            rawBetData.setEffectiveTurnover(rawBetData.getBetAmount());
        }

        //vendorSettleTime will be re-process if empty return from vendor
        if (ObjectUtils.isEmpty(rawBetData.getVendorSettleTime())) {
            //vendorSettleTime = vendorBetTime
            rawBetData.setVendorSettleTime(rawBetData.getVendorBetTime());
        }

        //resultTime will be re-process if empty return from vendor
        if (ObjectUtils.isEmpty(rawBetData.getResultTime())) {
            //resultTime = vendorSettleTime
            rawBetData.setResultTime(rawBetData.getVendorSettleTime());
        }

        return rawBetData;
    }

    /**
     * Creates a Result bet record of the given RawResultBet entity object.
     * This function will also populate default values of certain fields.
     *
     * @param unsettledBet, rawResultBet, rawSettledBet entity object containing information of a single result bet
     * @return RawResultBet entity object after a successful save
     */
    public SettledBet convertRawUnsettledBetForWalletTransaction(UnsettledBet unsettledBet)
            throws MergedBetDataIntegrityException {

        try {
            SettledBet unsettledData = new SettledBet();
            BeanUtils.copyProperties(unsettledData, unsettledBet);

            return unsettledData;

        } catch (IllegalAccessException illegalAccessException) {
            throw new MergedBetDataIntegrityException("getValueFromObject invalid : " + illegalAccessException.getMessage());

        } catch (InvocationTargetException invocationTargetException) {
            throw new MergedBetDataIntegrityException("copyProperties invalid : " + invocationTargetException.getMessage());
        }
    }

    /**
     * Get values that is not null from the object.
     *
     * @param object, fieldName, entity object containing information of a single bet
     * @return Object entity after getting all the non-null properties
     */
    private Object getValueFromObject(SettledBet object, String fieldName) throws IllegalAccessException {
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
