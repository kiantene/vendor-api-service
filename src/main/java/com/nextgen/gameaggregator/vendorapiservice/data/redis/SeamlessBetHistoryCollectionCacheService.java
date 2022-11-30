package com.nextgen.gameaggregator.vendorapiservice.data.redis;

import com.nextgen.gameaggregator.vendorapiservice.data.couchbase.entity.seamlessbethistorycollection.SeamlessBetHistoryCollection;
import com.nextgen.gameaggregator.vendorapiservice.data.couchbase.entity.seamlessbethistorycollection.SeamlessBetHistoryCollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;


/**
 *
 * @author Who
 * @description Redis cache service for User Role Permission Web Service.
 */
@Service
@CacheConfig(cacheNames = { "Cache" })
public class SeamlessBetHistoryCollectionCacheService {
	@Autowired
	private SeamlessBetHistoryCollectionRepository seamlessBetHistoryCollectionRepository;

    @Autowired
	private RedisTemplate redisTemplate;

	@Cacheable(value = "seamlessBetHistoryCollectionById")
	public SeamlessBetHistoryCollection findById(String id) {
		SeamlessBetHistoryCollection entity = seamlessBetHistoryCollectionRepository.findById(id).orElse(null);
		return entity;
	}

	public void save(String key, String hashKey, Object value) {
		redisTemplate.opsForHash().put(key, hashKey, value);
	}

	public Object get(String key, String hashKey) {
		return redisTemplate.opsForHash().get(key, hashKey);
	}

}
