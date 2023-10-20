package com.nextgen.gameaggregator.service;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Service
@Slf4j
public class BetIdempotentLogService {
    @Autowired
    private RawBetIdempotentLogRepository rawBetIdempotentLogRepository;

    @CachePut(value = "RawBetIdempotentLogs", key = "{#betResultData.vendorBetId, #betResultData.roundId, #betResultData.betAmount, #betResultData.winAmount, #betResultData.jackpotAmount}", cacheManager = "cacheManager")
    public RawBetIdempotentLog create(BetResultData betResultData, BigDecimal balance) {
        RawBetIdempotentLog entity = new RawBetIdempotentLog();
        String betIdempotentId = this.generateBetIdempotentId(betResultData.getVendorBetId(), betResultData.getRoundId(), betResultData.getBetAmount(), betResultData.getWinAmount(), betResultData.getJackpotAmount());
        Integer dailyDate = 0;

        //if both vendorBetTime and vendorSettleTime is empty, then save as 0 for dailyDate (PP would be fall under this scenario)
        if (betResultData.getVendorBetTime() != null && betResultData.getVendorSettleTime() != null) {
            dailyDate = this.unixTimeToDailyDateInteger(betResultData.getVendorBetTime(), betResultData.getVendorSettleTime());

        }

        entity.setId(betIdempotentId);
        entity.setDailyDate(dailyDate);
        entity.setBalance(balance);

        rawBetIdempotentLogRepository.save(entity);
        return entity;
    }

    private Integer unixTimeToDailyDateInteger(Long vendorBetTime, Long vendorSettleTime) {
        Long vendorBetTiming = (vendorBetTime != null) ? vendorBetTime : vendorSettleTime;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        String formattedDate = sdf.format(new Date(vendorBetTiming));

        return (Integer.parseInt(formattedDate));

    }

    private String generateBetIdempotentId(String vendorBetId, String roundId, BigDecimal betAmount, BigDecimal winAmount, BigDecimal jackpotAmount) {
        String betIdempotentId = vendorBetId + roundId + betAmount + winAmount + jackpotAmount;
        betIdempotentId = DigestUtils.md5Hex(betIdempotentId).toUpperCase();

        return betIdempotentId;

    }

    //2 hours differences in millie seconds
    public Long getTimingDifference() {
        Long twoHoursInMillis = 2L * 60L * 60L * 1000L;
        return twoHoursInMillis;

    }

    @Cacheable(value = "RawBetIdempotentLogs", key = "{#betResultData.vendorBetId, #betResultData.roundId, #betResultData.betAmount, #betResultData.winAmount, #betResultData.jackpotAmount}", cacheManager = "cacheManager")
    public RawBetIdempotentLog checkExistsWithDailyDate(BetResultData betResultData) {
        String betIdempotentId = this.generateBetIdempotentId(betResultData.getVendorBetId(), betResultData.getRoundId(), betResultData.getBetAmount(), betResultData.getWinAmount(), betResultData.getJackpotAmount());
        Integer dailyDate = this.unixTimeToDailyDateInteger(betResultData.getVendorBetTime(), betResultData.getVendorSettleTime());

        RawBetIdempotentLog betIdempotentLog = rawBetIdempotentLogRepository.findByIdAndDailyDate(betIdempotentId, dailyDate);
        return betIdempotentLog;

    }

    @Cacheable(value = "RawBetIdempotentLogs", key = "{#betResultData.vendorBetId, #betResultData.roundId, #betResultData.betAmount, #betResultData.winAmount, #betResultData.jackpotAmount}", cacheManager = "cacheManager")
    public RawBetIdempotentLog checkExists(BetResultData betResultData) {
        String betIdempotentId = this.generateBetIdempotentId(betResultData.getVendorBetId(), betResultData.getRoundId(), betResultData.getBetAmount(), betResultData.getWinAmount(), betResultData.getJackpotAmount());

        return rawBetIdempotentLogRepository.findById(betIdempotentId).orElse(null);

    }
}
