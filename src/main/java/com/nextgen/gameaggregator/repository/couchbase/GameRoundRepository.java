package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    public void appendTxn(String docId, RoundTxn roundTxn, long cas) {
        collection.mutateIn(docId,
                List.of(
                        MutateInSpec.arrayAppend("transactions", List.of(roundTxn)),
                        MutateInSpec.increment("txnCount", 1)
                ),
                MutateInOptions.mutateInOptions().cas(cas)
        );
    }

    public void updateTxn(String docId, int idx, Map<String, Object> updates) {
        if (updates.isEmpty()) return;

        final String base = "transactions[" + idx + "]";
        var specs = new ArrayList<MutateInSpec>();

        updates.forEach((k, v) -> specs.add(MutateInSpec.upsert(base + "." + k, v)));

        collection.mutateIn(docId, specs);
    }

    public void applyTxnDelta(TxnDelta d, Duration ttl) {
        final String base = "transactions[" + d.idx() + "]";
        var specs = new ArrayList<MutateInSpec>();

        specs.add(MutateInSpec.upsert(base + ".gaBetId", d.gaBetId()));
        d.status().ifPresent(s -> specs.add(MutateInSpec.replace(base + ".status", s.name())));
        if (d.timeField().isPresent() && d.timeValueUtc().isPresent()) {
            String path = switch (d.timeField().get()) {
                case SENT_AT -> base + ".sentAt";
                case DONE_AT -> base + ".doneAt";
            };
            specs.add(MutateInSpec.upsert(path, d.timeValueUtc().get()));
        }

        if (d.isSettled()) {
            specs.add(MutateInSpec.upsert("state", GameRoundState.SETTLED.name()));
        }
        if (d.isEnded()) {
            specs.add(MutateInSpec.upsert("isEnded", true));
        }

        if (d.lastBalance().isPresent()) {
            specs.add(MutateInSpec.upsert("lastBalance", d.lastBalance().get().toPlainString()));
        }

        // For aggregates: read current totals once to compute new values (CAS protects write)
        if (d.betDelta().isPresent() || d.winDelta().isPresent() || d.isSettled()) {
            var gr = collection.get(d.docId());
            var round = gr.contentAs(GameRound.class);

            BigDecimal bet = Optional.ofNullable(round.getBetAmount()).orElse(BigDecimal.ZERO);
            BigDecimal win = Optional.ofNullable(round.getWinAmount()).orElse(BigDecimal.ZERO);

            if (d.betDelta().isPresent()) bet = bet.add(d.betDelta().get());
            if (d.winDelta().isPresent()) win = win.add(d.winDelta().get());

            specs.add(MutateInSpec.replace("betAmount", bet.toPlainString()));
            specs.add(MutateInSpec.replace("winAmount", win.toPlainString()));

            var opts = d.isEnded()
                    ? MutateInOptions.mutateInOptions().cas(gr.cas()).expiry(ttl)
                    : MutateInOptions.mutateInOptions().cas(gr.cas());

            collection.mutateIn(d.docId(), specs, opts);
        } else {
            collection.mutateIn(d.docId(), specs);
        }
    }

    public void updateRoundState(String docId, GameRoundState state) {
        collection.mutateIn(docId, List.of(MutateInSpec.upsert("state", state.name())));
    }
}
