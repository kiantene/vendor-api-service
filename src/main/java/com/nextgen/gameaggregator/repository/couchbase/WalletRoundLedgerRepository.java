package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundTxnRef;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.service.data.couchbase.WalletTxnRetention;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * KV repository for {@link WalletRoundLedger} documents in the {@code wallet_round_ledgers} collection.
 *
 * <p>{@link #appendTxnRef} mirrors {@code GameRoundRepository.appendTxn}'s CAS-aware
 * {@code arrayAppend} + {@code increment} pattern, extended with an additional
 * {@code upsert("vendorLastBalance", ...)} so the latest balance lands atomically with the ref.
 */
@Repository
public class WalletRoundLedgerRepository {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final Collection collection;

    public WalletRoundLedgerRepository(@Qualifier("walletRoundLedgersCollection") Collection collection) {
        this.collection = collection;
    }

    public Optional<KvDoc<WalletRoundLedger>> findById(String id) {
        try {
            GetResult result = collection.get(id);
            WalletRoundLedger content = result.contentAs(WalletRoundLedger.class);
            return Optional.of(new KvDoc<>(id, result.cas(), content));
        } catch (DocumentNotFoundException e) {
            return Optional.empty();
        }
    }

    public KvDoc<WalletRoundLedger> insert(WalletRoundLedger content) {
        String id = content.getId();
        MutationResult res = collection.insert(id, content);
        return new KvDoc<>(id, res.cas(), content);
    }

    /**
     * Atomic CAS-aware mutation: append the ref to {@code transactions[]}, increment
     * {@code txnCount} by 1, and (when known) upsert {@code vendorLastBalance}. All in one
     * {@code mutateIn}.
     *
     * <p>{@code vendorLastBalance} is null on the operator-failure path (no vendor view). The
     * upsert is skipped in that case so the {@code arrayAppend} + {@code increment} still land —
     * a {@code null.toPlainString()} NPE would otherwise abort the whole {@code mutateIn} and, since
     * the caller swallows exceptions here, silently drop the failed-credit ref from the ledger.
     */
    public void appendTxnRef(String docId, WalletRoundTxnRef ref, BigDecimal vendorLastBalance, long cas) {
        List<MutateInSpec> specs = new ArrayList<>(List.of(
                MutateInSpec.arrayAppend("transactions", List.of(ref)),
                MutateInSpec.increment("txnCount", 1)
        ));
        if (vendorLastBalance != null) {
            specs.add(MutateInSpec.upsert("vendorLastBalance", vendorLastBalance.toPlainString()));
        }
        collection.mutateIn(docId, specs,
                MutateInOptions.mutateInOptions().cas(cas).timeout(TIMEOUT));
    }

    /**
     * CAS-aware flip to {@code state = SETTLED} and {@code isEnded = true}, which also starts the
     * ledger's retention clock — the direct analogue of {@code GameRound.setRoundState}'s TTL.
     *
     * <p>Unlike the txn documents, the ledger is an aggregate and not an idempotency key, so
     * expiring it costs nothing beyond a staler no-op balance: {@code CreditPolicy} falls back to
     * the debit's own round ledger and then to the balance recorded on the debit itself. A round
     * that never closes never reaches here and so never expires, by design — an unclosed round is
     * an open position.
     */
    public void markSettled(String docId, long cas) {
        collection.mutateIn(docId,
                List.of(
                        MutateInSpec.upsert("state", GameRoundState.SETTLED.name()),
                        MutateInSpec.upsert("isEnded", true)
                ),
                MutateInOptions.mutateInOptions()
                        .cas(cas)
                        .timeout(TIMEOUT)
                        .expiry(WalletTxnRetention.TERMINAL_TTL)
        );
    }
}
