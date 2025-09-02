package com.nextgen.gameaggregator.service.business;

import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.service.data.GameTransactionDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameTransactionService {
    private final GameTransactionDataService data;

    public Optional<GameTransaction> get(GameTransaction txn) {
        var doc = data.findById(txn.getId());

        if (doc == null) return Optional.empty();

        return Optional.of(doc);
    }

    public void save(GameTransaction txn) {
        if (TxnStatus.SENT == txn.getStatus()) {
            txn.setSentAt(getNow());
        }
        data.insert(txn);
    }

    public void markSuccess(GameTransaction txn, BigDecimal balance) {
        data.updateStatus(txn, balance, TxnStatus.SUCCESS);
    }

    public void markSent(GameTransaction txn) {
        txn.setStatus(TxnStatus.SENT);
        data.update(txn);
    }

    private String getNow() {
        return LocalTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    public void deleteById(String id) {
        data.deleteById(id);
    }
}
