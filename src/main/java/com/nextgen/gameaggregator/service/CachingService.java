package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.RawResultBet;
import com.nextgen.gameaggregator.entity.RawUnsettledBet;
import com.nextgen.gameaggregator.util.ApiSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CachingService {

    @CachePut(value = "BetHistories", key = "{#betHistory.roundId, #betHistory.vendorGameId, #betHistory.vendorPlayerId}", cacheManager = "cacheManager")
    public BetHistory updateBetHistoriesCaching(BetHistory betHistory) {
        return betHistory;
    }

    @CachePut(value = "UnsettledBet", key = "{#rawUnsettledBet.roundId, #rawUnsettledBet.vendorGameId, #rawUnsettledBet.vendorPlayerId}", cacheManager = "cacheManager")
    public RawUnsettledBet updateUnsettledBetCaching(RawUnsettledBet rawUnsettledBet) {
        return rawUnsettledBet;
    }

    @CachePut(value = "ResultBet", key = "{#rawResultBet.roundId, #rawResultBet.vendorGameId, #rawResultBet.vendorPlayerId}", cacheManager = "cacheManager")
    public RawResultBet updateResultBetCaching(RawResultBet rawResultBet) {
        return rawResultBet;
    }

    @CacheEvict(value = "UnsettledBet", key = "{#rawUnsettledBet.roundId, #rawUnsettledBet.vendorGameId, #rawUnsettledBet.vendorPlayerId}", cacheManager = "cacheManager")
    public RawUnsettledBet deleteUnsettledBetCaching(RawUnsettledBet rawUnsettledBet) {
        return rawUnsettledBet;
    }

    @CachePut(value = "ResultBet", key = "{#rawResultBet.roundId, #rawResultBet.vendorGameId, #rawResultBet.vendorPlayerId}", cacheManager = "cacheManager")
    public RawResultBet deleteResultBetCaching(RawResultBet rawResultBet) {
        return rawResultBet;
    }


    @CacheEvict(value = "BetHistories", key = "{#betHistory.roundId, #betHistory.vendorGameId, #betHistory.vendorPlayerId}", cacheManager = "cacheManager")
    public BetHistory deleteBetHistoriesCaching(BetHistory betHistory) {
        return betHistory;
    }

}
