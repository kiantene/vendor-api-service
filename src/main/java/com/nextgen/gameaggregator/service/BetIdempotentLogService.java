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

    @CachePut(value = "RawBetIdempotentLog", key = "{#betResultData.vendorBetId, #betResultData.roundId, #betResultData.betAmount, #betResultData.winAmount, #betResultData.jackpotAmount}", cacheManager = "cacheManager")
    public RawBetIdempotentLog create(BetResultData betResultData, BigDecimal balance) {
        RawBetIdempotentLog entity = new RawBetIdempotentLog();
        String betIdempotentId = this.generateBetIdempotentId(betResultData);

        entity.setId(betIdempotentId);
        entity.setBalance(balance);

        rawBetIdempotentLogRepository.save(entity);
        return entity;
    }

    private String generateBetIdempotentId(BetResultData betResultData) {

        Map<String, String> map = new HashMap<>();
        map.put("vendorBetId", betResultData.getVendorBetId());
        map.put("roundId", betResultData.getRoundId());
        map.put("betAmount", (betResultData.getBetAmount() == null) ? "0" : betResultData.getBetAmount().toString());
        map.put("winAmount", (betResultData.getWinAmount() == null) ? "0" : betResultData.getWinAmount().toString());
        map.put("jackpotAmount", (betResultData.getJackpotAmount() == null) ? "0" : betResultData.getJackpotAmount().toString());

        String betIdempotentId = betResultData.getVendorBetId() + "_" + betResultData.getRoundId() + "_" + betResultData.getBetAmount() + "_" +
                betResultData.getWinAmount() + "_" + betResultData.getJackpotAmount();
        betIdempotentId = DigestUtils.md5Hex(betIdempotentId).toUpperCase();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(map);
            betIdempotentId = DigestUtils.md5Hex(json).toUpperCase();

        } catch (JsonProcessingException e) {
            log.error("generateBetIdempotentId ERROR : " + e.getMessage());

        }

        return betIdempotentId;

    }

    //2 hours differences in millie seconds
    public Long getTimingDifference() {
        Long twoHoursInMillis = 2L * 60L * 60L * 1000L;
        return twoHoursInMillis;

    }

    @Cacheable(value = "RawBetIdempotentLog", key = "{#betResultData.vendorBetId, #betResultData.roundId, #betResultData.betAmount, #betResultData.winAmount, #betResultData.jackpotAmount}", cacheManager = "cacheManager")
    public RawBetIdempotentLog checkExists(BetResultData betResultData) {
        String betIdempotentId = this.generateBetIdempotentId(betResultData);

        return rawBetIdempotentLogRepository.findById(betIdempotentId).orElse(null);

    }
}
