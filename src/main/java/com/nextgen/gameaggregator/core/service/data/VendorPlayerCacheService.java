package com.nextgen.gameaggregator.core.service.data;

import com.couchbase.client.java.Collection;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.repository.ga.writer.VendorPlayerRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
class VendorPlayerCacheService extends CouchbaseCacheService<VendorPlayer> {
    private final VendorPlayerRepository repository;

    public VendorPlayerCacheService(@Qualifier("vendorPlayerCollection") Collection vendorPlayerCollection,
                                    VendorPlayerRepository repository) {

        super(vendorPlayerCollection, VendorPlayer.class);
        this.repository = repository;
    }

    public VendorPlayer getByUsername(String username) {
        String key = buildCacheKey("username", username);
        return retrieve(key,
                () -> repository.findByUsername(username),
                Duration.ofMinutes(120));
    }
}
