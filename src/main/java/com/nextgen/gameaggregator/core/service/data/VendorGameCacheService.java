package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.cache.couchbase.CouchbaseCacheFactory;
import com.nextgen.core.cache.couchbase.CouchbaseCacheService;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
class VendorGameCacheService extends CouchbaseCacheService<VendorGame> {
    private final VendorGameRepository repository;

    public VendorGameCacheService(CouchbaseCacheFactory factory,
                                  VendorGameRepository repository) {

        super(factory, VendorGame.class);
        this.repository = repository;
    }

    @Override
    protected Map<String, Duration> getTtlMap() {
        return Map.of(
                ttlKey("id"), Duration.ofMinutes(120),
                ttlKey("vendorGameCodeAndVendorId"), Duration.ofMinutes(120)
        );
    }

    public VendorGame getById(Integer id) {
        String key = buildCacheKey("id", id);
        return get(key, () -> repository.findById(id).orElse(null));
    }

    public VendorGame getByVendorGameCodeAndVendorId(String vendorGameCode, Integer vendorId) {
        String key = buildCacheKey("vendorGameCodeAndVendorId", vendorGameCode, vendorId);
        return get(key, () -> repository.findByVendorGameCodeAndVendorId(vendorGameCode, vendorId));
    }
}
