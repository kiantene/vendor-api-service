package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.cache.couchbase.CouchbaseCacheFactory;
import com.nextgen.core.cache.couchbase.CouchbaseCacheService;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.repository.ga.writer.VendorPlayerRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
class VendorPlayerCacheService extends CouchbaseCacheService<VendorPlayer> {
    private final VendorPlayerRepository repository;

    public VendorPlayerCacheService(CouchbaseCacheFactory factory,
                                    VendorPlayerRepository repository) {

        super(factory, VendorPlayer.class);
        this.repository = repository;
    }

    @Override
    protected Map<String, Duration> getTtlMap() {
        return Map.of(
                ttlKey("username"), Duration.ofMinutes(120)
        );
    }

    public VendorPlayer getByUsername(String username) {
        String key = buildCacheKey("username", username);
        return get(key, () -> repository.findByUsername(username));
    }
}
