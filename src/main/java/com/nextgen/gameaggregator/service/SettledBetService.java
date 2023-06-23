package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.repository.RawSettledBetRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SettledBetService {

    @Autowired
    RawSettledBetRepository rawSettledBetRepository;

    @Caching(put = {
            @CachePut(value = "SettledBet", key = "{#settledBet.externalTransactionId, #settledBet.vendorPlayerId}", cacheManager = "cacheManager"),
            @CachePut(value = "SettledBet", key = "{#settledBet.vendorBetId, #settledBet.roundId, #settledBet.vendorId, #settledBet.vendorPlayerId}", cacheManager = "cacheManager")
    })
    public SettledBet save(SettledBet settledBet, String rawData) {

        if (settledBet.getResettleNum() == null) {
            settledBet.setResettleNum(0);
        }

        if (settledBet.getRawData() == null) {
            settledBet.setRawData(DigestUtils.md5Hex(rawData));
        }

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

    @Cacheable(value = "SettledBet", key = "{#vendorBetId, #roundId, #vendorId, #vendorPlayerId}", cacheManager = "cacheManager")
    public SettledBet getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(String vendorBetId, String roundId, Integer vendorId, Long vendorPlayerId) throws BetNotFoundException {

        SettledBet settledBet = rawSettledBetRepository.findByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(vendorBetId, roundId, vendorId, vendorPlayerId);
        if (settledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId");
        }

        return settledBet;
    }

    /**
     * Delete settled bet record of the given settleBet entity object after successful inserted into kafka.
     *
     * @param settledBet entity object containing information of a single settled bet
     */
    public void delete(SettledBet settledBet) {
        try {
            rawSettledBetRepository.delete(settledBet);
        } catch (Exception e) {
            log.warn("Couchbase Delete SettledBet.exception -> vendorBetId = " + settledBet.getVendorBetId() + "& roundId = " + settledBet.getRoundId());
        }
    }

//    public boolean isBetExists(String vendorBetId, String roundId, Integer vendorId, Long vendorPlayerId) {
//        try {
//            settledBet = this.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(vendorBetId, roundId, vendorId, vendorPlayerId);
//            isBetExistsForSettledBet = true;
//        } catch (BetNotFoundException betNotFoundException) {
//            isBetExistsForSettledBet = false;
//        }
//    }
}
