package com.nextgen.gameaggregator.custodianseamless.service;


import com.nextgen.gameaggregator.custodianseamless.exception.DuplicateReferenceIdException;
import com.nextgen.gameaggregator.custodianseamless.operator.dto.TraceIdRequest;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import com.nextgen.gameaggregator.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.repository.ga.writer.RawTransferHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransferHistoryService {

    @Autowired
    private RawTransferHistoryRepository rawTransferHistoryRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Cacheable(value = "TraceIds", key = "{#traceId, #agentId}", cacheManager = "cacheManager", unless = "#result == null")
    public TraceIdRequest checkTraceIdExists(String traceId, Integer agentId) throws DuplicateRequestException {
        String cacheKey = "TraceIds::" + traceId + "," + agentId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            throw new DuplicateRequestException("traceId :" + traceId + " existing within 2 hours ");
        }else{
            return new TraceIdRequest(traceId, agentId);
        }
    }

    @Cacheable(value = "RawTransferHistories", key = "{#referenceId, #agentId}", cacheManager = "cacheManager", unless = "#result == null")
    public TraceIdRequest checkReferenceIdExists(String referenceId, Integer agentId) throws DuplicateReferenceIdException {
        String cacheKey = "RawTransferHistories::" + referenceId + "," + agentId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            throw new DuplicateReferenceIdException("referenceId :" + referenceId + " existing within 7 days ");
        }else{
            return null;
        }
    }

    @Cacheable(value = "RawTransferHistories", key = "{#referenceId, #agentId}", cacheManager = "cacheManager", unless = "#result == null")
    public RawTransferHistory checkTransactionExists(String referenceId, Integer  agentId) {
        return rawTransferHistoryRepository.findById(referenceId).orElse(null);
    }

    @CachePut(value = "RawTransferHistories", key = "{#referenceId, #agentPlayer.agentId}", cacheManager = "cacheManager")
    public RawTransferHistory preGenerateRawTransferHistory(String referenceId, AgentPlayer agentPlayer, Currency currency, Integer transactionType, BigDecimal transferAmount){
        return new RawTransferHistory(referenceId, agentPlayer, currency, transactionType, transferAmount);
    }
    @CachePut(value = "RawTransferHistories", key = "{#rawTransferHistory.id, #rawTransferHistory.agentId}", cacheManager = "cacheManager")
    public RawTransferHistory updateRawTransferHistory(RawTransferHistory rawTransferHistory){
        return rawTransferHistory;
    }

    public void saveRawTransferHistory(RawTransferHistory rawTransferHistory){
        rawTransferHistoryRepository.save(rawTransferHistory) ;
    }


    //region transfer wallet
//    @Cacheable(value = "RawTransferHistories", key = "{#referenceId, #agentId}", cacheManager = "cacheManager", unless = "#result == null")
//    public RawTransferHistory checkTransferHistoryExists(String referenceId,  Integer agentId) throws DuplicateReferenceIdException {
////        // Optional::orElse(null) is safe here, as null is handled by the unless condition
////        //return optionalTransferHistory.orElse(null);
////
////        String cacheKey = "RawTransferHistories::" + referenceId + "," + agentId;
////        Boolean keyExists = redisTemplate.hasKey(cacheKey);
////
////        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
////            throw new DuplicateReferenceIdException("referenceId :" + referenceId + " existing within 7 days ");
////        }else{
////            Optional<RawTransferHistory> optionalTransferHistory = rawTransferHistoryRepository.findById(referenceId);
////
////            if(optionalTransferHistory.isPresent()){
////                throw new DuplicateReferenceIdException("referenceId :" + referenceId + " existing within 7 days ");
////            }
////        }
//        return null;
//
//    }

//    public void checkUniqueTraceIdRequest(String traceId, Integer agentId) throws DuplicateRequestException {
//
//        TraceIdRequest traceIdRequest = cachingService.checkTraceIdExists(traceId, agentId);
//        System.err.println("traceIdRequest");
//        System.err.println( new Gson().toJson(traceIdRequest));
//        if(traceIdRequest != null){
//            throw new DuplicateRequestException("traceId :" + traceId + " existing within 2 hours ");
//        }else{
//            traceIdRequest =  cachingService.createTraceIdRequest(traceId, agentId);
//        }
//
////        String cacheKey = "TraceId:" + traceId + ":agentId:" + agentId;
////        // Check if the key exists in the Redis cache
////        Boolean keyExists = redisTemplate.hasKey(cacheKey);
////        // Return true if the key is not found, otherwise return false
////        if (Boolean.FALSE.equals(keyExists)) {
////            // Get the current date and time
////            LocalDateTime currentTime = LocalDateTime.now();
////            // Define the desired date-time format
////            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
////            // Format the current time using the defined formatter
////            String formattedTime = currentTime.format(formatter);
////            // If the key is not found, store it in the Redis cache and set a TTL (time-to-live)
////            redisTemplate.opsForValue().set(cacheKey, formattedTime);
////            redisTemplate.expire(cacheKey, 2, TimeUnit.HOURS); // Set your desired TTL
////
////        } else {
////            throw new DuplicateRequestException("traceId :" + traceId + " existing within 2 hours ");
////        }
//
//    }

//    public RawTransferHistory checkUniqueReferenceId(String referenceId, Integer agentId) throws DuplicateReferenceIdException {
//        RawTransferHistory rawTransferHistory = cachingService.checkTransferHistoryExists(referenceId, agentId);
//        System.err.println("rawTransferHistory");
//        System.err.println( new Gson().toJson(rawTransferHistory));
//        if(rawTransferHistory != null){
//            throw new DuplicateReferenceIdException("referenceId :" + referenceId + " existing within 7 days ");
//        }
//        return rawTransferHistory;
//    }



}
