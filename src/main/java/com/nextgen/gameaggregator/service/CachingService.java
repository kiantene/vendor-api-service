package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@Slf4j
public class CachingService {

    @CachePut(value = "BetHistories", key = "{#betHistory.roundId, #betHistory.vendorGameId, #betHistory.vendorPlayerId}", cacheManager = "cacheManager")
    public BetHistory updateBetHistoriesCaching(BetHistory betHistory) {
        return betHistory;
    }

    @CachePut(value = "UnsettledBet", key = "{#rawUnsettledBet.vendorBetId, #rawUnsettledBet.roundId, #rawUnsettledBet.vendorGameId, #rawUnsettledBet.vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBet updateUnsettledBetCaching(UnsettledBet unsettledBet) {
        return unsettledBet;
    }

    @CachePut(value = "ResultBet", key = "{#rawResultBet.vendorBetId, #rawResultBet.roundId, #rawResultBet.vendorGameId, #rawResultBet.vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBetResult updateResultBetCaching(UnsettledBetResult unsettledBetResult) {
        return unsettledBetResult;
    }

    @CacheEvict(value = "UnsettledBet", key = "{#vendorBetId, #roundId, #vendorLineId, #vendorPlayerId}", cacheManager = "cacheManager")
    public void deleteUnsettledBetCaching(String vendorBetId, String roundId, Integer vendorLineId, Long vendorPlayerId) {}

    @CacheEvict(value = "ResultBet", key = "{#vendorBetId, #roundId, #vendorLineId, #vendorPlayerId}", cacheManager = "cacheManager")
    public void deleteResultBetCaching(String vendorBetId, String roundId, Integer vendorLineId, Long vendorPlayerId) {}

    @CacheEvict(value = "UnsettledBetWithGameId", key = "{#vendorBetId, #roundId, #vendorGameId, #vendorPlayerId}", cacheManager = "cacheManager")
    public void deleteUnsettledBetByGameIdCaching(String vendorBetId, String roundId, Integer vendorGameId, Long vendorPlayerId) {}

    @CachePut(value = "SettledBet", key = "{#rawSettledBet.vendorBetId, #rawSettledBet.roundId, #rawSettledBet.vendorGameId, #rawSettledBet.vendorPlayerId}", cacheManager = "cacheManager")
    public SettledBet updateSettledBetCaching(SettledBet settledBet) {
        return settledBet;
    }

    @CacheEvict(value = "SettledBet", key = "{#rawSettledBet.vendorBetId, #rawSettledBet.roundId, #rawSettledBet.vendorGameId, #rawSettledBet.vendorPlayerId}", cacheManager = "cacheManager")
    public SettledBet deleteSettledBetCaching(SettledBet settledBet) {
        return settledBet;
    }


    @CacheEvict(value = "BetHistories", key = "{#betHistory.roundId, #betHistory.vendorGameId, #betHistory.vendorPlayerId}", cacheManager = "cacheManager")
    public BetHistory deleteBetHistoriesCaching(BetHistory betHistory) {
        return betHistory;
    }

    @Cacheable(value = "promoData", key = "{#vendorPlayerId, #vendorBetId, #vendorRoundId}", cacheManager = "cacheManager")
    public UnsettledBet storeProcessPromoToRedis(Long vendorPlayerId, String vendorBetId, String vendorRoundId, String traceId) {
        UnsettledBet unsettledBet = new UnsettledBet();
        unsettledBet.setVendorBetId(vendorBetId);
        unsettledBet.setRoundId(vendorRoundId);
        unsettledBet.setInternalTransactionId(traceId);
        unsettledBet.setVendorPlayerId(vendorPlayerId);
        return unsettledBet;
    }

    @CachePut(value = "playerBalance", key = "{#gameSession.vendorPlayerId, #gameSession.agentId}", cacheManager = "cacheManager")
    public PlayerBalance storePlayerLatestBalanceToRedis(GameSession gameSession, BigDecimal balance) {
        PlayerBalance playerBalance = new PlayerBalance();
        playerBalance.setAgentId(gameSession.getAgentId());
        playerBalance.setAgentPlayerId(gameSession.getAgentPlayerId());
        playerBalance.setAgentPlayerUsername(gameSession.getAgentPlayerUsername());
        playerBalance.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        playerBalance.setVendorPlayerId(gameSession.getVendorPlayerId());
        playerBalance.setCurrencyCode(gameSession.getCurrencyCode());
        playerBalance.setCreateTime(Instant.now().toEpochMilli());
        playerBalance.setBalance(balance);
        return playerBalance;
    }

    @Cacheable(value = "playerBalance", key = "{#gameSession.vendorPlayerId, #gameSession.agentId}", cacheManager = "cacheManager")
    public PlayerBalance getPlayerLatestBalanceFromRedis(GameSession gameSession) {
        PlayerBalance playerBalance = new PlayerBalance();
        playerBalance.setAgentId(gameSession.getAgentId());
        playerBalance.setAgentPlayerId(gameSession.getAgentPlayerId());
        playerBalance.setAgentPlayerUsername(gameSession.getAgentPlayerUsername());
        playerBalance.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        playerBalance.setVendorPlayerId(gameSession.getVendorPlayerId());
        playerBalance.setCurrencyCode(gameSession.getCurrencyCode());
        playerBalance.setCreateTime(Instant.now().toEpochMilli());
        return playerBalance;
    }

}
