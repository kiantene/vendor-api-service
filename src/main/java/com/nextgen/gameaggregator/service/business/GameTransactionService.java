package com.nextgen.gameaggregator.service.business;

import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.service.data.GameRoundDataService;
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
    private final GameTransactionDataService txnDataService;
    private final GameRoundDataService roundDataService;

    public Optional<GameTransaction> get(GameTransaction txn) {
        var doc = txnDataService.findById(txn.getId());

        if (doc == null) return Optional.empty();

        return Optional.of(doc);
    }

    public GameTransaction save(GameTransaction txn) {
        if (TxnStatus.NEW == txn.getStatus()) {
            txn.setCreatedAt(getNow());
        } else if (TxnStatus.SENT == txn.getStatus()) {
            txn.setSentAt(getNow());
        }
        txnDataService.insert(txn);
        return txn;
    }

    public void markSent(GameTransaction txn) {
        txn.setSentAt(getNow());
        txn.setStatus(TxnStatus.SENT);
        txnDataService.update(txn);

        // construct round info and save txn as success unless failed then update
    }

    public void markSuccess(GameTransaction txn, BigDecimal balance) {
        txn.setDoneAt(getNow());
        txnDataService.updateStatus(txn, balance, TxnStatus.SUCCESS);
    }

    private String getNow() {
        return TIME_FORMATTER.format(Instant.now());
    }

    public void deleteById(String id) {
        txnDataService.deleteById(id);
    }
}
