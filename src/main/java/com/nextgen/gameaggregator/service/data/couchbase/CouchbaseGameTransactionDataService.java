package com.nextgen.gameaggregator.service.data.couchbase;

import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.repository.couchbase.GameTransactionRepository;
import com.nextgen.gameaggregator.service.data.GameTransactionDataService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    public void updateStatus(GameTransaction doc, BigDecimal balance, TxnStatus status) {
        Duration ttl = null;
        Map<String, Object> updates = new HashMap<>();

        updates.put("status", status.name());

        if (balance != null) {
            updates.put("balance", balance.toPlainString()); // convert to String to avoid loss of precision
        }

        if (TxnStatus.SENT == status) {
            updates.put("sentAt", getNow());
        }

        if (TxnStatus.SUCCESS == status) {
            updates.put("doneAt", getNow());
            ttl = Duration.ofHours(6);
        }

        repo.update(doc.getId(), updates, ttl);
    }

    @Override
    public void deleteById(String id) {
        repo.delete(id);
    }

    private String getNow() {
        return LocalTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}
