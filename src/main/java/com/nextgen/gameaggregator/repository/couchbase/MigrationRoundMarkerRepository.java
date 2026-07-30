package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
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

    public Optional<RoundMarker> get(String key) {
        try {
            return Optional.of(collection.get(key).contentAs(RoundMarker.class));
        } catch (DocumentNotFoundException e) {
            return Optional.empty();
        }
    }
}
