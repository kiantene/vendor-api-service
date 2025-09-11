package com.nextgen.gameaggregator.service.data.couchbase;

import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.repository.couchbase.GameTransactionRepository;
import com.nextgen.gameaggregator.service.data.GameTransactionDataService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class CouchbaseGameTransactionDataService implements GameTransactionDataService {
    private final GameTransactionRepository repo;

    public CouchbaseGameTransactionDataService(GameTransactionRepository repo) {
        this.repo = repo;
    }

    @Override
    public GameTransaction findById(String id) {
        return repo.findById(id)
                .map(KvDoc::getPayload)
                .orElse(null);
    }

    @Override
    public void insert(GameTransaction doc) {
        KvDoc<GameTransaction> kvDoc = repo.insert(doc);
    }

    @Override
    public void update(GameTransaction doc) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", doc.getUsername());
        updates.put("roundId", doc.getRoundId());
        updates.put("gameCode", doc.getGameCode());
        updates.put("currency", doc.getCurrency());
        updates.put("status", doc.getStatus().name());
        updates.put("state", doc.getState().name());

        updateIfNotNull(updates, "betAmount", doc.getBetAmount());
        updateIfNotNull(updates, "betTime", doc.getBetTime());
        updateIfNotNull(updates, "winAmount", doc.getWinAmount());
        updateIfNotNull(updates, "settleTime", doc.getSettleTime());

        if (TxnStatus.SENT == doc.getStatus()) {
            updates.put("sentAt", doc.getSentAt());
        }

        repo.update(doc.getId(), updates, null);
    }

    @Override
    public void updateStatus(GameTransaction txn, BigDecimal balance, TxnStatus status) {
        Duration ttl = null;
        Map<String, Object> updates = new HashMap<>();

        updates.put("status", status.name());
        updateIfNotNull(updates, "idx", txn.getIdx());
        updateIfNotNull(updates, "balance", balance);
        updateIfNotNull(updates, "gaBetId", txn.getGaBetId());

        if (TxnStatus.SENT == status) {
            updates.put("sentAt", txn.getSentAt());
        } else if (TxnStatus.SUCCESS == status || TxnStatus.ERROR == status) {
            updates.put("doneAt", txn.getDoneAt());
        }

        if (GameRoundState.SETTLED == txn.getState()) {
            updates.put("state", txn.getState().name());
            ttl = Duration.ofHours(3);
        }

        repo.update(txn.getId(), updates, ttl);
    }

    @Override
    public void deleteById(String id) {
        repo.delete(id);
    }

    private void updateIfNotNull(Map<String, Object> map, String field, Object value) {
        if (value != null) {
            if (value instanceof BigDecimal decimal) {
                map.put(field, decimal.toPlainString()); // convert to String to avoid loss of precision
            } else {
                map.put(field, value);
            }
        }
    }
}
