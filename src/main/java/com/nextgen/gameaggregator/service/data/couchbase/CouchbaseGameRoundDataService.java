package com.nextgen.gameaggregator.service.data.couchbase;

import com.couchbase.client.core.error.CasMismatchException;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.repository.couchbase.GameRoundRepository;
import com.nextgen.gameaggregator.service.data.GameRoundDataService;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class CouchbaseGameRoundDataService implements GameRoundDataService {
    private final GameRoundRepository repo;

    public CouchbaseGameRoundDataService(GameRoundRepository repo) {
        this.repo = repo;
    }

    @Override
    public KvDoc<GameRound> findById(String id) {
        return repo.findById(id)
                .orElse(null);
    }

    @Override
    public void insert(GameRound round) {
        repo.insert(round);
    }

    @Override
    public void appendTxn(String docId, RoundTxn roundTxn, long cas) {
        runWithCasRetry(() -> repo.appendTxn(docId, roundTxn, cas));
    }

    @Override
    public void updateTxn(String docId, int idx, Map<String, Object> updates) {
        repo.updateTxn(docId, idx, updates);
    }

    @Override
    public void setRoundState(String docId, GameRoundState state) {
        Duration ttl = Duration.ofHours(3);
        repo.updateRoundState(docId, state, ttl);
    }

    @Override
    public GameRound applyTxnDelta(TxnDelta delta, Duration ttl) {
        return repo.applyTxnDelta(delta, ttl);
    }

    private static void runWithCasRetry(Runnable op) {
        int attempts = 0;
        while (true) {
            try {
                op.run();
                return;
            } catch (CasMismatchException e) {
                if (++attempts >= 3) throw e;
            }
        }
    }
}
