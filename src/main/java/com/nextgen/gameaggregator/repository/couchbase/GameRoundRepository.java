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
import com.nextgen.gameaggregator.enums.TxnType;
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

    public GameRound applyTxnDelta(TxnDelta d, Duration ttl) {
        var specs = new ArrayList<MutateInSpec>();

        addTransactionFieldUpdates(specs, d);

        addGameStateUpdates(specs, d);

        var gr = collection.get(d.docId());
        var round = gr.contentAs(GameRound.class);
        var updatedAmounts = calculateUpdatedAmounts(round, d);

        addAmountsUpdate(specs, updatedAmounts);

        var opts = MutateInOptions.mutateInOptions().cas(gr.cas());

        if (d.isEnded()) {
            opts.expiry(ttl);
        }

        collection.mutateIn(d.docId(), specs, opts);

        return round;
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

    private void addAmountsUpdate(List<MutateInSpec> specs, AggregateAmounts amounts) {
        specs.add(MutateInSpec.upsert("betAmount", amounts.bet().toPlainString()));
        specs.add(MutateInSpec.upsert("winAmount", amounts.win().toPlainString()));
        specs.add(MutateInSpec.upsert("jackpotAmount", amounts.jackpot().toPlainString()));
    }

    private AggregateAmounts calculateUpdatedAmounts(GameRound gameRound, TxnDelta d) {
        BigDecimal bet = Optional.ofNullable(gameRound.getBetAmount()).orElse(BigDecimal.ZERO);
        BigDecimal win = Optional.ofNullable(gameRound.getWinAmount()).orElse(BigDecimal.ZERO);
        BigDecimal jackpot = Optional.ofNullable(gameRound.getJackpotAmount()).orElse(BigDecimal.ZERO);

        // Apply deltas
        if (d.betDelta().isPresent()) {
            bet = bet.add(d.betDelta().get());
        }
        if (d.winDelta().isPresent()) {
            win = win.add(d.winDelta().get());
        }
        if (d.jackpotDelta().isPresent()) {
            jackpot = jackpot.add(d.jackpotDelta().get());
        }

        // Update the GameRound object in-place
        gameRound.setBetAmount(bet);
        gameRound.setWinAmount(win);
        gameRound.setJackpotAmount(jackpot);

        return new AggregateAmounts(bet, win, jackpot);
    }

    private record AggregateAmounts(BigDecimal bet, BigDecimal win, BigDecimal jackpot) {}
}
