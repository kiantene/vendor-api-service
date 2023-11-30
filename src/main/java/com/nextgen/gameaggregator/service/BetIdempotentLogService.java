package com.nextgen.gameaggregator.service;

import com.couchbase.client.core.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.repository.RawBetIdempotentLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class BetIdempotentLogService {
    @Autowired
    private RawBetIdempotentLogRepository rawBetIdempotentLogRepository;

    public RawBetIdempotentLog create(BetResultData betResultData, BigDecimal balance, GameSession gameSession) {
        RawBetIdempotentLog entity = new RawBetIdempotentLog();
        String betIdempotentId = this.generateBetIdempotentId(betResultData, gameSession);

        entity.setId(betIdempotentId);
        entity.setBalance(balance);

        rawBetIdempotentLogRepository.save(entity);
        return entity;
    }

    private String generateBetIdempotentId(BetResultData betResultData, GameSession gameSession) {

        String betIdempotentId = betResultData.getVendorBetId() + "_" + betResultData.getRoundId() + "_" + gameSession.getVendorPlayerUsername();
        betIdempotentId = DigestUtils.md5Hex(betIdempotentId).toUpperCase();

        return betIdempotentId;

    }

    //2 hours differences in millie seconds
    public Long getTimingDifference() {
        Long twoHoursInMillis = 2L * 60L * 60L * 1000L;
        return twoHoursInMillis;

    }

    public Long getTimingDifferenceForStillProcessing() {
        Long fiveSecondsInMillis = 5L * 1000L;
        return fiveSecondsInMillis;

    }

    public Long compareWithExistingTimingDifference(Long createdDate) {

        Long existingTime = System.currentTimeMillis();
        Long timingDifference = existingTime - createdDate;
        return timingDifference;

    }

    @Cacheable(value = "RawBetIdempotentLog", key = "{#betResultData.vendorBetId, #betResultData.roundId, #betResultData.betAmount, #betResultData.winAmount, #betResultData.jackpotAmount, #gameSession.vendorPlayerUsername}", cacheManager = "cacheManager", unless = "#result == null")
    public RawBetIdempotentLog checkExists(BetResultData betResultData, GameSession gameSession) {
        String betIdempotentId = this.generateBetIdempotentId(betResultData, gameSession);

        return rawBetIdempotentLogRepository.findById(betIdempotentId).orElse(null);

    }
}
