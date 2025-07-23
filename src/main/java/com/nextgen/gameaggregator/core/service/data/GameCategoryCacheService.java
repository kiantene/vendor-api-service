package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.cache.couchbase.CouchbaseCacheFactory;
import com.nextgen.core.cache.couchbase.CouchbaseCacheService;
import com.nextgen.gameaggregator.entity.ga.GameCategory;
import com.nextgen.gameaggregator.repository.ga.writer.GameCategoryRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
class GameCategoryCacheService extends CouchbaseCacheService<GameCategory> {
    private final GameCategoryRepository repository;

    public GameCategoryCacheService(CouchbaseCacheFactory factory,
                                    GameCategoryRepository repository) {

        super(factory, GameCategory.class);
        this.repository = repository;
    }

    @Override
    protected Map<String, Duration> getTtlMap() {
        return Map.of(
                ttlKey("id"), Duration.ofMinutes(120)
        );
    }

    public GameCategory getById(Integer id) {
        String key = buildCacheKey("id", id);
        return get(key, () -> repository.findById(id).orElse(null));
    }
}
