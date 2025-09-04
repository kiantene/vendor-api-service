package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GameRoundRepository {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final Collection collection;

    public GameRoundRepository(@Qualifier("gameRoundsCollection") Collection collection) {
        this.collection = collection;
    }

    public String buildId(Integer vendorId, String roundId) {
        return vendorId + "::" + roundId;
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

    public void appendTxn(String docId, RoundTxn roundTxn, BigDecimal newBetAmount, BigDecimal newWinAmount, long cas) {
        collection.mutateIn(docId,
                List.of(
                        MutateInSpec.arrayAppend("transactions", List.of(roundTxn)),
                        MutateInSpec.increment("txnCount", 1),
                        MutateInSpec.replace("betAmount", newBetAmount),
                        MutateInSpec.replace("winAmount", newWinAmount)
                ),
                MutateInOptions.mutateInOptions().cas(cas)
        );
    }

    public void updateTxnStatus(String docId, int idx, TxnStatus status, boolean settle, Duration ttlIfSettled) {
        String base = "transactions[" + idx + "]";
        List<MutateInSpec> specs = new ArrayList<>();
        specs.add(MutateInSpec.replace(base + ".status", status.name()));

        if (TxnStatus.SUCCESS == status) {
            specs.add(MutateInSpec.upsert(base + ".doneAt", LocalTime.now(ZoneOffset.UTC).format(TIME_FMT)));
        }
        if (settle) {
            specs.add(MutateInSpec.upsert("state", GameRoundState.SETTLED.name()));
        }

        MutateInOptions opts = settle
                ? MutateInOptions.mutateInOptions().expiry(ttlIfSettled)
                : MutateInOptions.mutateInOptions();

        collection.mutateIn(docId, specs, opts);
    }

    public void updateRoundState(String docId, GameRoundState state) {
        collection.mutateIn(docId, List.of(MutateInSpec.upsert("state", state.name())));
    }
}
