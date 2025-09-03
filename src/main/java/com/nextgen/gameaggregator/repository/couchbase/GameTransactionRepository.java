package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@Repository
public class GameTransactionRepository {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final Collection collection;

    public GameTransactionRepository(@Qualifier("gameTransactionsCollection") Collection collection) {
        this.collection = collection;
    }

    public Optional<KvDoc<GameTransaction>> findById(String id) {
        try {
            GetResult result = collection.get(id);

            GameTransaction content = result.contentAs(GameTransaction.class);

            return Optional.of(new KvDoc<>(id, result.cas(), content));
        } catch (DocumentNotFoundException e) {
            return Optional.empty();
        }
    }

    public KvDoc<GameTransaction> insert(GameTransaction content) {
        String id = content.getId();

        MutationResult res = collection.insert(id, content);
        return new KvDoc<>(id, res.cas(), content);
    }

    public void delete(String id) {
        collection.remove(id);
    }

    public void update(String id, Map<String, Object> updateList, Duration ttl) {
        var specs = new ArrayList<MutateInSpec>();
        updateList.forEach((k, v) -> specs.add(MutateInSpec.upsert(k, v)));

        var options = MutateInOptions.mutateInOptions().timeout(TIMEOUT);
        if (ttl != null) {
            options.expiry(ttl);
        }

        collection.mutateIn(id, specs, options);
    }
}
