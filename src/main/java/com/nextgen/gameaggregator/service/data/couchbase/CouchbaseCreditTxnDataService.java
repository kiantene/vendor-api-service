package com.nextgen.gameaggregator.service.data.couchbase;

import com.couchbase.client.core.error.DocumentExistsException;
import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.repository.couchbase.CreditTxnRepository;
import com.nextgen.gameaggregator.service.data.CreditTxnDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Couchbase impl of {@link CreditTxnDataService}. Backed by {@link CreditTxnRepository}.
 *
 * <p>Symmetric to {@code CouchbaseDebitTxnDataService} but without CAS state transitions —
 * a {@link CreditTxn}'s {@code state} (SETTLED or REFUNDED) is terminal-at-creation and
 * gets serialized to disk on the initial {@link #insertOrGet}. The settle-mode analytics
 * fields and refund-mode linkage are likewise written on insert (the processor populates
 * them before calling {@link #insertOrGet}). Subsequent status transitions only touch
 * {@code status}, {@code vendorBalance}, {@code doneAt}, and {@code exception}.
 */
@Service
@Slf4j
public class CouchbaseCreditTxnDataService implements CreditTxnDataService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);

    private final CreditTxnRepository repo;

    public CouchbaseCreditTxnDataService(CreditTxnRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<KvDoc<CreditTxn>> insertOrGet(CreditTxn doc) {
        try {
            repo.insert(doc);
            return Optional.empty();
        } catch (DocumentExistsException e) {
            String id = doc.getId();
            log.info("CreditTxn doc already exists, returning existing: {}", id);
            return repo.findById(id)
                    .or(() -> {
                        throw new IllegalStateException(
                                "DocumentExistsException but CreditTxn not found: " + id, e);
                    });
        }
    }

    @Override
    public KvDoc<CreditTxn> findById(String id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public void markSent(CreditTxn doc, long cas) {
        doc.setStatus(TxnStatus.SENT);
        doc.setSentAt(getNow());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", TxnStatus.SENT.name());
        updates.put("sentAt", doc.getSentAt());
        // Deliberately NOT retried on CasMismatchException: a mismatch means another caller already
        // re-armed this txn and is calling the operator. Retrying would produce a second operator
        // call for the same transaction. The caller translates the mismatch into a duplicate.
        if (cas == WalletIdempotencyResolver.NO_CAS) {
            repo.update(doc.getId(), updates, null);
        } else {
            repo.updateWithCas(doc.getId(), updates, cas);
        }
    }

    @Override
    public void markSuccess(CreditTxn doc, BigDecimal vendorBalance) {
        doc.setStatus(TxnStatus.SUCCESS);
        doc.setVendorBalance(vendorBalance);
        doc.setDoneAt(getNow());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", TxnStatus.SUCCESS.name());
        updates.put("doneAt", doc.getDoneAt());
        updateIfNotNull(updates, "vendorBalance", vendorBalance);
        // Terminal: the credit is done. markError deliberately gets no TTL — it is the re-arm
        // state, and a credit nobody retries is an open item someone needs to see.
        repo.update(doc.getId(), updates, WalletTxnRetention.TERMINAL_TTL);
    }

    @Override
    public void markError(CreditTxn doc, String operatorStatusCode) {
        doc.setStatus(TxnStatus.ERROR);
        doc.setException(operatorStatusCode);
        doc.setDoneAt(getNow());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", TxnStatus.ERROR.name());
        updates.put("doneAt", doc.getDoneAt());
        updateIfNotNull(updates, "exception", operatorStatusCode);
        repo.update(doc.getId(), updates, null);
    }

    private static String getNow() {
        return TIME_FORMATTER.format(Instant.now());
    }

    private static void updateIfNotNull(Map<String, Object> map, String field, Object value) {
        if (value == null) return;
        if (value instanceof BigDecimal decimal) {
            map.put(field, decimal.toPlainString());
        } else {
            map.put(field, value);
        }
    }
}
