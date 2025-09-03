package com.nextgen.gameaggregator.service.business;

import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.service.data.GameTransactionDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameTransactionService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);
    private final GameTransactionDataService data;

    public Optional<GameTransaction> get(GameTransaction txn) {
        var doc = data.findById(txn.getId());

        if (doc == null) return Optional.empty();

        return Optional.of(doc);
    }

    public void save(GameTransaction txn) {
        if (TxnStatus.NEW == txn.getStatus()) {
            txn.setCreatedAt(getNow());
        } else if (TxnStatus.SENT == txn.getStatus()) {
            txn.setSentAt(getNow());
        }
        data.insert(txn);
    }

    public void markSuccess(GameTransaction txn, BigDecimal balance) {
        txn.setDoneAt(getNow());
        data.updateStatus(txn, balance, TxnStatus.SUCCESS);
    }

    public void markSent(GameTransaction txn) {
        txn.setSentAt(getNow());
        txn.setStatus(TxnStatus.SENT);
        data.update(txn);
    }

    private String getNow() {
        return TIME_FORMATTER.format(Instant.now());
    }

    public void deleteById(String id) {
        data.deleteById(id);
    }
}
