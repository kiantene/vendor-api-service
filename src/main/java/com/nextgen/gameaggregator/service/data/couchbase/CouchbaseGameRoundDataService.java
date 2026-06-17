package com.nextgen.gameaggregator.service.data.couchbase;

import com.couchbase.client.core.error.DocumentExistsException;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.repository.couchbase.GameRoundRepository;
import com.nextgen.gameaggregator.service.data.GameRoundDataService;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
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
    public Optional<KvDoc<GameRound>> insertOrGet(GameRound round) {
        try {
            insert(round);
            return Optional.empty();
        } catch (DocumentExistsException e) {
            String docId = round.getId();
            log.error("Round Document already exists: " + docId, e);
            return repo.findById(docId)
                    .or(() -> {
                        // Invariant violation — insert says it exists, but get cannot find it
                        throw new IllegalStateException("DocumentExistsException but GameRound not found: " + docId, e);
                    });
        }
    }

    @Override
    public int appendTxn(String docId, RoundTxn roundTxn) {
        // No CAS retry needed — appendTxn is concurrency-safe at the server
        // (commutative arrayAppend + atomic counter increment).
        return repo.appendTxn(docId, roundTxn);
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
}
