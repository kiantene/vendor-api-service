package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.RawBetIdempotentLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.repository.ga.writer.RawBetIdempotentLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

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

    @Cacheable(value = "RawBetIdempotentLog", key = "{#betResultData.vendorBetId, #betResultData.roundId, #betResultData.betAmount, #betResultData.winAmount, #betResultData.jackpotAmount, #gameSession.vendorPlayerUsername}", cacheManager = "cacheManager", unless = "#result == null")
    public RawBetIdempotentLog checkExistsForQa(SettledBet settledBet) {
        String betIdempotentId = this.generateBetIdempotentIdForQa(settledBet);
        return rawBetIdempotentLogRepository.findById(betIdempotentId).orElse(null);

    }

    private String generateBetIdempotentIdForQa(SettledBet settledBet) {

        String betIdempotentId = settledBet.getVendorBetId() + "_" + settledBet.getRoundId() + "_" + settledBet.getVendorPlayerId();
        betIdempotentId = DigestUtils.md5Hex(betIdempotentId).toUpperCase();

        return betIdempotentId;

    }

    public RawBetIdempotentLog createForQa(SettledBet settledBet) {
        RawBetIdempotentLog entity = new RawBetIdempotentLog();
        String betIdempotentId = this.generateBetIdempotentIdForQa(settledBet);

        entity.setId(betIdempotentId);
        entity.setBalance(settledBet.getBalance());

        rawBetIdempotentLogRepository.save(entity);
        return entity;
    }

    public void idempotentCheck(String id) throws DuplicateRequestException {
        Optional<RawBetIdempotentLog> rawBetIdempotentLogOptional = rawBetIdempotentLogRepository.findById(id);

        if (rawBetIdempotentLogOptional.isPresent()) { // id is found, which means this request has been sent before
            throw new DuplicateRequestException(id);
        }

        // create idempotent log if new request
        RawBetIdempotentLog rawBetIdempotentLog = new RawBetIdempotentLog();
        rawBetIdempotentLog.setId(id);

        rawBetIdempotentLogRepository.save(rawBetIdempotentLog);
    }
}
