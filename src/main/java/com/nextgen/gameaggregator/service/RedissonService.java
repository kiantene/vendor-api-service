package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import lombok.Getter;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Getter
@Service
public class RedissonService {

    private final RedissonClient redissonClient;
    private final HttpService httpService;

    @Autowired
    public RedissonService(RedissonClient redissonClient, HttpService httpService) {
        this.redissonClient = redissonClient;
        this.httpService = httpService;
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

    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    // Method to safely delete a lock (only if held by the current client)
    public void deleteLockSafely(RLock lock) {

        if (lock == null) {
            //do nothing
            //if no lock exist, out from this function
        } else if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    // Method to wait until the lock is released
    public void waitUntilLockReleased(RLock lock, HttpRequestLog httpRequestLog) throws TransactionStillProcessingException {
        try {
            while (lock.remainTimeToLive() != -2) {
                if (!lock.isHeldByCurrentThread()) {
                    RBucket<Object> bucket = redissonClient.getBucket(lock.getName()); //check if key still exist
                    if (!bucket.isExists()) { //if key is entirely disappeared
                        break;//Lock has been released, break out of the loop
                    }
                }
            }
            throw new TransactionStillProcessingException();
        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            httpService.logError(httpRequestLog, transactionStillProcessingException);
        }
    }
}