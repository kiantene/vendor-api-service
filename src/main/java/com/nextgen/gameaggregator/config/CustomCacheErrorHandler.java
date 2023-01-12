package com.nextgen.gameaggregator.config;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

public class CustomCacheErrorHandler implements CacheErrorHandler {
    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        // Do nothing or log error
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        // Do nothing or log error
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        // Do nothing or log error
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        // Do nothing or log error
    }
}
