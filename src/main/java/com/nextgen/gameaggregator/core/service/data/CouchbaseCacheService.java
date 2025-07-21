package com.nextgen.gameaggregator.core.service.data;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.UpsertOptions;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.function.Supplier;

public abstract class CouchbaseCacheService<T> {

    private final Collection collection;
    private final ObjectMapper objectMapper;
    private final Class<T> clazz;

    protected CouchbaseCacheService(Collection collection, ObjectMapper objectMapper, Class<T> clazz) {
        this.collection = collection;
        this.objectMapper = objectMapper;
        this.clazz = clazz;
    }

    protected abstract String buildCacheKey(String id);

    public T getById(String id) {
        try {
            GetResult result = collection.get(buildCacheKey(id));
            return objectMapper.readValue(result.contentAsBytes(), clazz);
        } catch (Exception e) {
            return null; // treat as cache miss
        }
    }

    public void upsert(String id, T value, Duration ttl) {
        try {
            String key = buildCacheKey(id);
            byte[] json = objectMapper.writeValueAsBytes(value);
            collection.upsert(key, json, UpsertOptions.upsertOptions().expiry(ttl));
        } catch (Exception e) {
            // optionally log or swallow
        }
    }

    public void remove(String id) {
        try {
            collection.remove(buildCacheKey(id));
        } catch (Exception e) {
            // log or ignore if key doesn't exist
        }
    }

    public T retrieve(String id, Supplier<T> fallbackSupplier, Duration ttl) {
        T cached = getById(id);
        if (cached != null) return cached;

        T value = fallbackSupplier.get();
        if (value != null) {
            upsert(id, value, ttl); // TODO: wrap in async
        }
        return value;
    }
}
