package com.nextgen.gameaggregator.core.service.data;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.UpsertOptions;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.function.Supplier;

public abstract class CouchbaseCacheService<T> {

    private final String DELIMITER = "::";
    private final Collection collection;
    private final ObjectMapper objectMapper;
    private final Class<T> clazz;
    private final String prefix;

    protected CouchbaseCacheService(Collection collection, ObjectMapper objectMapper, Class<T> clazz) {
        this.collection = collection;
        this.objectMapper = objectMapper;
        this.clazz = clazz;
        this.prefix = toKebabCase(clazz.getSimpleName());
    }

    protected String buildCacheKey(Object... parts) {
        StringBuilder sb = new StringBuilder(prefix);

        for (Object part : parts) {
            sb.append(DELIMITER);
            sb.append(part instanceof String
                    ? ((String) part).trim().toLowerCase()
                    : part);
        }

        return sb.toString();
    }

    public T getById(String cacheKey) {
        try {
            GetResult result = collection.get(cacheKey);
            if (result == null) return null;
            return result.contentAs(clazz);
        } catch (Exception e) {
            return null; // treat as cache miss
        }
    }

    public void upsert(String cacheKey, T value, Duration ttl) {
        try {
            collection.upsert(cacheKey, value, UpsertOptions.upsertOptions().expiry(ttl));
        } catch (Exception e) {
            // optionally log or swallow
        }
    }

    public void evict(String cacheKey) {
        try {
            collection.remove(cacheKey);
        } catch (Exception e) {
            // log or ignore if key doesn't exist
        }
    }

    public T retrieve(String cacheKey, Supplier<T> fallbackSupplier, Duration ttl) {
        T cached = getById(cacheKey);
        if (cached != null) return cached;

        T value = fallbackSupplier.get();
        if (value != null) {
            upsert(cacheKey, value, ttl); // TODO: wrap in async
        }
        return value;
    }

    private String toKebabCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}
