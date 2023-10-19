package com.nextgen.gameaggregator.vendor.ezugi.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockManager {

    private final ConcurrentHashMap<String, Lock> usernameLocks = new ConcurrentHashMap<>();

    public Lock getLockForUsername(String username) {
        return usernameLocks.computeIfAbsent(username, k -> new ReentrantLock());
    }
}