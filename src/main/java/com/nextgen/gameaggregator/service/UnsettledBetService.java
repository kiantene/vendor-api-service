package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.UnsettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.repository.RawUnsettledBetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UnsettledBetService {
    @Autowired
    private RawUnsettledBetRepository rawUnsettledBetRepository;

    /**
     * Retrieve an unsettled bet transaction record based on vendor's round Id, game Id, and player Id
     *
     * @param roundId        Vendor's round Id
     * @param vendorGameId   vendor game id within Game Aggregator System
     * @param vendorPlayerId Id of the record in VendorPlayer
     * @return unsettled bet entity object containing all information of a single unsettled Bet
     * @throws BetNotFoundException If no bet record is found
     */
    @Cacheable(value = "UnsettledBet", key = "{#vendorBetId, #roundId, #vendorGameId, #vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBet getUnsettledBetByRoundId(String vendorBetId, String roundId, Integer vendorGameId, Long vendorPlayerId) throws BetNotFoundException {

        String mergeId = vendorBetId + '_' + roundId + '_' + vendorGameId + '_' + vendorPlayerId;
        UnsettledBet unsettledBet = null;

        unsettledBet = rawUnsettledBetRepository.findById(mergeId).orElse(null);
        if (unsettledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find round Id: " + roundId);
        }

        return unsettledBet;
    }

    @CachePut(value = "UnsettledBet", key = "{#unsettledBet.vendorBetId, #unsettledBet.roundId, #unsettledBet.vendorGameId, #unsettledBet.vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBet update(UnsettledBet unsettledBet) {
        rawUnsettledBetRepository.save(unsettledBet);
        return unsettledBet;
    }

    public UnsettledBet getByVendorPlayerIdAndExternalTransactionId(Long vendorPlayerId, String externalTransactionId) throws BetNotFoundException {
        UnsettledBet unsettledBet = rawUnsettledBetRepository.findByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);
        if (unsettledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find Vendor Player Id: " + vendorPlayerId + ", externalTransactionId: " + externalTransactionId);
        }

        return unsettledBet;
    }

    public UnsettledBet getByVendorIdAndExternalTransactionId(Integer vendorId, String externalTransactionId) throws BetNotFoundException {
        UnsettledBet unsettledBet = rawUnsettledBetRepository.findByVendorIdAndExternalTransactionId(vendorId, externalTransactionId);
        if (unsettledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find Vendor Id: " + vendorId + ", externalTransactionId: " + externalTransactionId);
        }

        return unsettledBet;
    }

    public List<UnsettledBet> getByRoundId(String roundId, Integer vendorGameId, Long vendorPlayerId) {
        List<UnsettledBet> unsettledBetLists = null;

        try {
            unsettledBetLists = rawUnsettledBetRepository.findByRoundIdAndVendorGameIdAndVendorPlayerId(roundId, vendorGameId, vendorPlayerId);

        } catch (Exception e) {
            //TODO ERROR HANDLING IF CONNECTION TO COUCHBASE IS FAILED
            log.error("getBetDataListByRoundId ERROR, details : " + e);
        }
        return unsettledBetLists;
    }
}
