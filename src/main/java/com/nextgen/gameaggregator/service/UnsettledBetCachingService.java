package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.repository.ga.writer.RawUnsettledBetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UnsettledBetCachingService {
    @Autowired
    private RawUnsettledBetRepository rawUnsettledBetRepository;
    @Autowired
    private KafkaService kafkaService;

    @Cacheable(value = "UnsettledBet", key = "{#vendorBetId, #roundId, #vendorGameId, #vendorPlayerId}", cacheManager = "cacheManager", unless = "#result == null")
    public UnsettledBet getUnsettledBetByRoundId(String vendorBetId, String roundId, Integer vendorGameId, Long vendorPlayerId) {

        String mergeId = vendorBetId + '_' + roundId + '_' + vendorGameId + '_' + vendorPlayerId;
        UnsettledBet unsettledBet = null;

        unsettledBet = rawUnsettledBetRepository.findById(mergeId).orElse(null);

        return unsettledBet;
    }

    @Cacheable(value = "UnsettledBet", key = "{#vendorBetId, #roundId, #vendorGameId, #vendorPlayerId}", cacheManager = "cacheManager", unless = "#result == null")
    public UnsettledBet getUnsettledBetByRoundIdWithErrorResponse(String vendorBetId, String roundId, Integer vendorGameId, Long vendorPlayerId) throws BetNotFoundException {

        String mergeId = vendorBetId + '_' + roundId + '_' + vendorGameId + '_' + vendorPlayerId;
        UnsettledBet unsettledBet = null;

        unsettledBet = rawUnsettledBetRepository.findById(mergeId).orElse(null);
        if (unsettledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find round Id: " + roundId);
        }

        return unsettledBet;
    }

    @Retryable(retryFor = {BetNotFoundException.class}, maxAttempts = 6, backoff = @Backoff(delay = 50))
    public UnsettledBet getTop1UnsettledBet(String roundId, Integer vendorGameId, Long vendorPlayerId) throws BetNotFoundException {
        UnsettledBet unsettledBet = rawUnsettledBetRepository.findTop1ByRoundIdAndVendorGameIdAndVendorPlayerIdOrderByCreateTimeDesc(roundId, vendorGameId, vendorPlayerId);
        if (unsettledBet == null) {
            throw new BetNotFoundException();
        }
        return unsettledBet;
    }

    @Recover
    public UnsettledBet recoverData(BetNotFoundException ex) {
        // Handle recovery logic here, such as returning a default value or logging the error
        UnsettledBet unsettledBet = new UnsettledBet();
        return unsettledBet;
    }
}
