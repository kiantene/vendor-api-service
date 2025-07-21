package com.nextgen.gameaggregator.core.service.data;

import com.couchbase.client.java.Collection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.repository.ga.writer.VendorPlayerRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
class VendorPlayerCacheService extends CouchbaseCacheService<VendorPlayer> {
    private final VendorPlayerRepository repository;
    private static final Duration TTL = Duration.ofMinutes(10);

    public VendorPlayerCacheService(Collection vendorPlayerCollection,
                                    ObjectMapper objectMapper,
                                    VendorPlayerRepository repository) {

        super(vendorPlayerCollection, objectMapper, VendorPlayer.class);
        this.repository = repository;
    }

    @Override
    protected String buildCacheKey(String username) {
        return "vendor-player::" + username;
    }

    public VendorPlayer getByUsername(String username) {
        return retrieve(username,
                () -> repository.findByUsername(username),
                TTL);
    }
}
