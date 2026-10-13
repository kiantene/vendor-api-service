package com.nextgen.gameaggregator.service.data.couchbase;

import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.core.error.DocumentExistsException;
import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.repository.couchbase.DebitTxnRepository;
import com.nextgen.gameaggregator.service.data.DebitTxnDataService;
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
 * Couchbase impl of {@link DebitTxnDataService}. Backed by {@link DebitTxnRepository}.
 *
 * <p>{@link #insertOrGet} catches {@link DocumentExistsException} and re-fetches via
 * {@code findById} — the load-bearing idempotency primitive. Mirrors
 * {@code CouchbaseGameRoundDataService.insertOrGet}.
 *
 * <p>Status transitions ({@code markSent / markSuccess / markError}) build a minimal
 * upsert map via {@link #updateIfNotNull} + {@link BigDecimal#toPlainString()} —
 * mirrors {@code CouchbaseGameTransactionDataService.updateIfNotNull}.
 *
 * <p>{@link #casUpdateState} writes the new state with CAS; retries up to 3 times on
 * {@link CasMismatchException}, mirroring {@code CouchbaseGameRoundDataService.runWithCasRetry}.
 */
@Service
@Slf4j
public class CouchbaseDebitTxnDataService implements DebitTxnDataService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);

    private final DebitTxnRepository repo;

    public CouchbaseDebitTxnDataService(DebitTxnRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<KvDoc<DebitTxn>> insertOrGet(DebitTxn doc) {
        try {
            repo.insert(doc);
            return Optional.empty();
        } catch (DocumentExistsException e) {
            String id = doc.getId();
            log.info("DebitTxn doc already exists, returning existing: {}", id);
            return repo.findById(id)
                    .or(() -> {
                        // Invariant violation — insert says it exists, but get cannot find it
                        throw new IllegalStateException(
                                "DocumentExistsException but DebitTxn not found: " + id, e);
                    });
        }
    }

    @Override
    public KvDoc<DebitTxn> findById(String id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public void markSent(DebitTxn doc, long cas) {
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
    public void markSuccess(DebitTxn doc, BigDecimal vendorBalance) {
        doc.setStatus(TxnStatus.SUCCESS);
        doc.setVendorBalance(vendorBalance);
        doc.setDoneAt(getNow());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", TxnStatus.SUCCESS.name());
        updates.put("doneAt", doc.getDoneAt());
        updateIfNotNull(updates, "vendorBalance", vendorBalance);
        repo.update(doc.getId(), updates, null);
    }

    @Override
    public void markError(DebitTxn doc, String operatorStatusCode) {
        doc.setStatus(TxnStatus.ERROR);
        doc.setException(operatorStatusCode);
        doc.setDoneAt(getNow());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", TxnStatus.ERROR.name());
        updates.put("doneAt", doc.getDoneAt());
        updateIfNotNull(updates, "exception", operatorStatusCode);
        repo.update(doc.getId(), updates, null);
    }

    @Override
    public void casUpdateState(KvDoc<DebitTxn> kvDoc, GameRoundState newState) {
        Map<String, Object> updates = Map.of("state", newState.name());
        // Terminal: this debit has been closed by a credit or refund, so the retention clock
        // starts here. A debit that merely SUCCEEDED is still open and gets no TTL.
        runWithCasRetry(() -> repo.updateWithCas(
                kvDoc.getId(), updates, kvDoc.getCas(), WalletTxnRetention.TERMINAL_TTL));
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
