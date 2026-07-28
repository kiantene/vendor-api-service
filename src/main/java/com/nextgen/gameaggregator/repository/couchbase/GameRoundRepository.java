package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInResult;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class GameRoundRepository {
    private final Collection collection;

    public GameRoundRepository(@Qualifier("gameRoundsCollection") Collection collection) {
        this.collection = collection;
    }

    public Optional<KvDoc<GameRound>> findById(String id) {
        try {
            GetResult result = collection.get(id);

            GameRound content = result.contentAs(GameRound.class);

            return Optional.of(new KvDoc<>(id, result.cas(), content));
        } catch (DocumentNotFoundException e) {
            return Optional.empty();
        }
    }

    public KvDoc<GameRound> insert(GameRound content) {
        String id = content.getId();

        MutationResult res = collection.insert(id, content);
        return new KvDoc<>(id, res.cas(), content);
    }

    // Spec order matters: index 1 is the txnCount increment whose post-mutation
    // value we read back. The arrayAppend at index 0 places the new RoundTxn at
    // (newCount - 1) atomically with the increment.
    //
    // No CAS is used here because both ops are concurrency-safe on their own:
    // arrayAppend is order-tolerant, and the counter increment is atomic and
    // returns each caller its own post-increment value. CAS would only produce
    // spurious mismatches under contention without preventing any real anomaly.
    private static final int APPEND_TXN_SPEC_INDEX_TXN_COUNT = 1;

    public int appendTxn(String docId, RoundTxn roundTxn) {
        MutateInResult result = collection.mutateIn(docId,
                List.of(
                        MutateInSpec.arrayAppend("transactions", List.of(roundTxn)),
                        MutateInSpec.increment("txnCount", 1)
                )
        );

        long newCount = result.contentAs(APPEND_TXN_SPEC_INDEX_TXN_COUNT, Long.class);
        return Math.toIntExact(newCount - 1L);
    }

    public void updateTxn(String docId, int idx, Map<String, Object> updates) {
        if (updates.isEmpty()) return;

        final String base = "transactions[" + idx + "]";
        var specs = new ArrayList<MutateInSpec>();

        updates.forEach((k, v) -> specs.add(MutateInSpec.upsert(base + "." + k, v)));

        collection.mutateIn(docId, specs);
    }

    public GameRound applyTxnDelta(TxnDelta d, Duration ttl) {
        var specs = new ArrayList<MutateInSpec>();

        // Per-slot writes — single-writer per transactions[idx], no contention.
        addTransactionFieldUpdates(specs, d);
        addPerSlotAmounts(specs, d);

        // Round-level writes — all idempotent upserts, atomic counters, or
        // monotonic flags. No read-modify-write, so no CAS is needed.
        addGameStateUpdates(specs, d);

        var opts = MutateInOptions.mutateInOptions();
        if (d.isEnded()) {
            opts.expiry(ttl);
        }

        collection.mutateIn(d.docId(), specs, opts);

        // Read post-mutation state for the return value. Not in any critical
        // section — it's just a fresh snapshot for the caller.
        return collection.get(d.docId()).contentAs(GameRound.class);
    }

    public void updateRoundState(String docId, GameRoundState state, Duration ttl) {
        var specs = new ArrayList<MutateInSpec>();

        specs.add(MutateInSpec.upsert("state", state.name()));

        if (ttl != null) {
            var opts = MutateInOptions.mutateInOptions().expiry(ttl);
            collection.mutateIn(docId, specs, opts);
        } else {
            collection.mutateIn(docId, specs);
        }
    }

    private void addTransactionFieldUpdates(List<MutateInSpec> specs, TxnDelta d) {
        final String basePath = "transactions[" + d.idx() + "]";

        // Always update gaBetId
        specs.add(MutateInSpec.upsert(basePath + ".gaBetId", d.gaBetId()));

        // Update status if present
        d.status().ifPresent(status ->
                specs.add(MutateInSpec.upsert(basePath + ".status", status.name())));

        // Update time fields if present
        addTimeFieldUpdate(specs, basePath, d);
    }

    private void addGameStateUpdates(List<MutateInSpec> specs, TxnDelta d) {
        if (d.isSettled()) {
            specs.add(MutateInSpec.upsert("state", GameRoundState.SETTLED.name()));
        }

        if (d.isEnded()) {
            specs.add(MutateInSpec.upsert("isEnded", true));
        }

        if (d.txnType() != null && d.txnType() == TxnType.BET) {
            specs.add(MutateInSpec.increment("betTxnCount", 1));
        }

        d.lastBalance().ifPresent(balance ->
                specs.add(MutateInSpec.upsert("lastBalance", balance.toPlainString())));

        d.effectiveTurnover().ifPresent(effectiveTurnover ->
                specs.add(MutateInSpec.upsert("effectiveTurnover", effectiveTurnover.toPlainString())));
    }

    private void addPerSlotAmounts(List<MutateInSpec> specs, TxnDelta d) {
        final String basePath = "transactions[" + d.idx() + "]";
        d.betDelta().ifPresent(v ->
                specs.add(MutateInSpec.upsert(basePath + ".betAmount", v.toPlainString())));
        d.winDelta().ifPresent(v ->
                specs.add(MutateInSpec.upsert(basePath + ".winAmount", v.toPlainString())));
        d.jackpotDelta().ifPresent(v ->
                specs.add(MutateInSpec.upsert(basePath + ".jackpotAmount", v.toPlainString())));
        d.cappedWinDelta().ifPresent(v ->
                specs.add(MutateInSpec.upsert(basePath + ".cappedWinAmount", v.toPlainString())));
        d.cappedJackpotDelta().ifPresent(v ->
                specs.add(MutateInSpec.upsert(basePath + ".cappedJackpotAmount", v.toPlainString())));
    }

    private void addTimeFieldUpdate(List<MutateInSpec> specs, String base, TxnDelta d) {
        if (d.timeField().isEmpty() || d.timeValueUtc().isEmpty()) {
            return;
        }

        d.timeField().ifPresent(timeField -> {
            String timePath = base + switch(timeField) {
                case SENT_AT -> ".sentAt";
                case DONE_AT -> ".doneAt";
            };

            specs.add(MutateInSpec.upsert(timePath, d.timeValueUtc().get()));
        });
    }

}
