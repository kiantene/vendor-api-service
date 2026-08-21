package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.InsertOptions;
import com.couchbase.client.java.kv.UpsertOptions;
import com.nextgen.gameaggregator.entity.couchbase.RoundMarker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class MigrationRoundMarkerRepository {

    private final Collection collection;

    public MigrationRoundMarkerRepository(@Qualifier("migrationRoundCollection") Collection collection) {
        this.collection = collection;
    }

    public void upsert(String key, RoundMarker marker, Duration ttl) {
        collection.upsert(key, marker, UpsertOptions.upsertOptions().expiry(ttl));
    }

    /**
     * Creates the marker only if the key is free.
     *
     * <p>Reports the outcome instead of throwing so the Couchbase exception vocabulary stays
     * inside this class; deciding what a lost race means is the caller's business.</p>
     *
     * @return {@code true} if this call created the marker, {@code false} if one already existed
     */
    public boolean insertIfAbsent(String key, RoundMarker marker, Duration ttl) {
        try {
            collection.insert(key, marker, InsertOptions.insertOptions().expiry(ttl));
            return true;
        } catch (DocumentExistsException alreadyClaimed) {
            return false;
        }
    }

    /**
     * Slides an existing marker's expiry without rewriting its body. Never creates one.
     *
     * @return {@code true} if a marker was found and extended, {@code false} if the key is free
     */
    public boolean touchIfPresent(String key, Duration ttl) {
        try {
            collection.touch(key, ttl);
            return true;
        } catch (DocumentNotFoundException absent) {
            return false;
        }
    }

    public Optional<RoundMarker> get(String key) {
        try {
            return Optional.of(collection.get(key).contentAs(RoundMarker.class));
        } catch (DocumentNotFoundException e) {
            return Optional.empty();
        }
    }
}
