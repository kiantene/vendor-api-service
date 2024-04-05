package com.nextgen.gameaggregator.service;

import lombok.Getter;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Getter
@Service
public class RedissonService {

    private final RedissonClient redissonClient;

    @Autowired
    public RedissonService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void storeValue(String key, String value) {
        // Using RBucket to store a single value in Redis
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    public String retrieveValue(String key) {
        // Using RBucket to retrieve a single value from Redis
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }
}