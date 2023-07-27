package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetNotFoundLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.repository.RawBetNotFoundLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BetNotFoundLogService {

    @Autowired
    RawBetNotFoundLogRepository rawBetNotFoundLogRepository;

    @CachePut(value = "BetNotFoundLog", key = "{#externalTransactionId, #vendorPlayerId}", cacheManager = "cacheManager")
    public BetNotFoundLog save(Long vendorPlayerId, String externalTransactionId, BetStatus betStatus) {

        BetNotFoundLog betNotFoundLog = new BetNotFoundLog();
        String betNotFoundLogId = vendorPlayerId + '_' + externalTransactionId;

        betNotFoundLog.setVendorPlayerId(vendorPlayerId);
        betNotFoundLog.setExternalTransactionId(externalTransactionId);
        betNotFoundLog.setStatus(betStatus.code);
        betNotFoundLog.setId(betNotFoundLogId);

        rawBetNotFoundLogRepository.save(betNotFoundLog);

        return betNotFoundLog;
    }

    @Cacheable(value = "BetNotFound", key = "{#externalTransactionId, #vendorPlayerId}", cacheManager = "cacheManager")
    public BetNotFoundLog getByVendorPlayerIdAndExternalTransactionId(Long vendorPlayerId, String externalTransactionId){

        BetNotFoundLog betNotFoundLog = rawBetNotFoundLogRepository.findByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);

        return betNotFoundLog;
    }

}
