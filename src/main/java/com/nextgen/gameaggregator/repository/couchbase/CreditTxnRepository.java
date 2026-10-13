package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * KV repository for {@link CreditTxn} documents in the {@code wallet_round_transactions} collection.
 * Mirrors {@code DebitTxnRepository}. {@code CreditTxn.state} is terminal-at-creation and needs no
 * CAS transition, but {@link #updateWithCas} exists for the {@code ERROR} re-arm, where two
 * concurrent retries must not both claim the same document.
 */
@Repository
public class CreditTxnRepository {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final Collection collection;

    public CreditTxnRepository(@Qualifier("walletRoundTransactionsCollection") Collection collection) {
        this.collection = collection;
    }

    public Optional<KvDoc<CreditTxn>> findById(String id) {
        try {
            GetResult result = collection.get(id);
            CreditTxn content = result.contentAs(CreditTxn.class);
            return Optional.of(new KvDoc<>(id, result.cas(), content));
        } catch (DocumentNotFoundException e) {
            return Optional.empty();
        }
    }

    public KvDoc<CreditTxn> insert(CreditTxn content) {
        String id = content.getId();
        MutationResult res = collection.insert(id, content);
        return new KvDoc<>(id, res.cas(), content);
    }

    /** Non-CAS upsert of the given fields. Used for status transitions (NEW→SENT→SUCCESS/ERROR). */
    public void update(String id, Map<String, Object> updates, Duration ttl) {
        if (updates.isEmpty()) return;
        var specs = new ArrayList<MutateInSpec>();
        updates.forEach((k, v) -> specs.add(MutateInSpec.upsert(k, v)));

        var options = MutateInOptions.mutateInOptions().timeout(TIMEOUT);
        if (ttl != null) {
            options.expiry(ttl);
        }
        collection.mutateIn(id, specs, options);
    }

    /**
     * CAS-protected upsert. Throws {@link com.couchbase.client.core.error.CasMismatchException}
     * when {@code cas} doesn't match the current server-side value.
     */
    public void updateWithCas(String id, Map<String, Object> updates, long cas) {
        if (updates.isEmpty()) return;
        var specs = new ArrayList<MutateInSpec>();
        updates.forEach((k, v) -> specs.add(MutateInSpec.upsert(k, v)));

        collection.mutateIn(id, specs,
                MutateInOptions.mutateInOptions().timeout(TIMEOUT).cas(cas));
    }
}
