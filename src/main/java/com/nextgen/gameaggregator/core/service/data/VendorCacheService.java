package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.cache.couchbase.CouchbaseCacheFactory;
import com.nextgen.core.cache.couchbase.CouchbaseCacheService;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.repository.ga.writer.VendorRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
class VendorCacheService extends CouchbaseCacheService<Vendor> {
    private final VendorRepository repository;

    public VendorCacheService(CouchbaseCacheFactory factory,
                              VendorRepository repository) {

        super(factory, Vendor.class);
        this.repository = repository;
    }

    @Override
    protected Map<String, Duration> getTtlMap() {
        return Map.of(
                ttlKey("id"), Duration.ofMinutes(120)
        );
    }

    public Vendor getById(Integer id) {
        String key = buildCacheKey("id", id);
        return get(key, () -> repository.findById(id).orElse(null));
    }
}
