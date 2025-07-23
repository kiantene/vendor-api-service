package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.cache.couchbase.CouchbaseCacheFactory;
import com.nextgen.core.cache.couchbase.CouchbaseCacheService;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.repository.ga.writer.CurrencyRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
class CurrencyCacheService extends CouchbaseCacheService<Currency> {
    private final CurrencyRepository repository;

    public CurrencyCacheService(CouchbaseCacheFactory factory,
                                CurrencyRepository repository) {

        super(factory, Currency.class);
        this.repository = repository;
    }

    @Override
    protected Map<String, Duration> getTtlMap() {
        return Map.of(
                ttlKey("id"), Duration.ofMinutes(120)
        );
    }

    public Currency getById(Integer id) {
        String key = buildCacheKey("id", id);
        return get(key, () -> repository.findById(id).orElse(null));
    }
}
