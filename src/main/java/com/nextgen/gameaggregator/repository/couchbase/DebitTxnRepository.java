package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * KV repository for {@link DebitTxn} documents in the {@code wallet_round_transactions} collection.
 * Shape mirrors {@code GameTransactionRepository} — direct {@code Collection} access, no Spring-Data sugar.
 */
@Repository
public class DebitTxnRepository {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final Collection collection;

    public DebitTxnRepository(@Qualifier("walletRoundTransactionsCollection") Collection collection) {
        this.collection = collection;
    }

    public Optional<KvDoc<DebitTxn>> findById(String id) {
        try {
            GetResult result = collection.get(id);
            DebitTxn content = result.contentAs(DebitTxn.class);
            return Optional.of(new KvDoc<>(id, result.cas(), content));
        } catch (DocumentNotFoundException e) {
            return Optional.empty();
        }
    }

    /** Throws {@link com.couchbase.client.core.error.DocumentExistsException} on doc-id collision. */
    public KvDoc<DebitTxn> insert(DebitTxn content) {
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
        updateWithCas(id, updates, cas, null);
    }

    /**
     * CAS-protected upsert that additionally sets the document's expiry when {@code ttl} is
     * non-null. Used to start the retention clock at the moment the debit becomes terminal —
     * see {@code WalletTxnRetention}.
     */
    public void updateWithCas(String id, Map<String, Object> updates, long cas, Duration ttl) {
        if (updates.isEmpty()) return;
        var specs = new ArrayList<MutateInSpec>();
        updates.forEach((k, v) -> specs.add(MutateInSpec.upsert(k, v)));

        var options = MutateInOptions.mutateInOptions().timeout(TIMEOUT).cas(cas);
        if (ttl != null) {
            options.expiry(ttl);
        }
        collection.mutateIn(id, specs, options);
    }
}
