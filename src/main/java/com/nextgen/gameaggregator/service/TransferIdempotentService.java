package com.nextgen.gameaggregator.service;


import com.nextgen.gameaggregator.exception.DuplicateRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class TransferIdempotentService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void checkUniqueTraceIdRequest(String traceId, Integer agentId) throws DuplicateRequestException {

        String cacheKey = "traceId:" + traceId + ":agentId:" + agentId;
        // Check if the key exists in the Redis cache
        Boolean keyExists = redisTemplate.hasKey(cacheKey);
        // Return true if the key is not found, otherwise return false
        if(Boolean.FALSE.equals(keyExists)){
            // Get the current date and time
            LocalDateTime currentTime = LocalDateTime.now();
            // Define the desired date-time format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // Format the current time using the defined formatter
            String formattedTime = currentTime.format(formatter);
            // If the key is not found, store it in the Redis cache and set a TTL (time-to-live)
            redisTemplate.opsForValue().set(cacheKey, formattedTime);
            redisTemplate.expire(cacheKey, 5, TimeUnit.MINUTES); // Set your desired TTL

        }else{
            throw new DuplicateRequestException();
        }

    }


}
