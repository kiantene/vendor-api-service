package com.nextgen.gameaggregator.service.data.couchbase;

import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.core.error.DocumentExistsException;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundTxnRef;
import com.nextgen.gameaggregator.repository.couchbase.WalletRoundLedgerRepository;
import com.nextgen.gameaggregator.service.data.WalletRoundLedgerDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Couchbase impl of {@link WalletRoundLedgerDataService}. Backed by {@link WalletRoundLedgerRepository}.
 *
 * <p>{@link #insertOrGet} mirrors {@code CouchbaseGameRoundDataService.insertOrGet} —
 * first writer wins on the {@code <vendorClassName>::<vendorPlayerUsername>::<vendorRoundId>}
 * doc id; subsequent debits on the same round get back the existing ledger.
 *
 * <p>{@link #appendTxnRef} retries up to 3 times on {@link CasMismatchException}.
 * On each retry, the caller's CAS becomes stale — but {@link WalletRoundLedgerRepository#appendTxnRef}
 * is structured so that the array-append + counter-increment + balance-upsert ops are
 * order-independent at the doc-version level: re-fetching the doc and retrying with the
 * new CAS converges.
 */
@Service
@Slf4j
public class CouchbaseWalletRoundLedgerDataService implements WalletRoundLedgerDataService {
    private final WalletRoundLedgerRepository repo;

    public CouchbaseWalletRoundLedgerDataService(WalletRoundLedgerRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<KvDoc<WalletRoundLedger>> insertOrGet(WalletRoundLedger ledger) {
        try {
            repo.insert(ledger);
            return Optional.empty();
        } catch (DocumentExistsException e) {
            String id = ledger.getId();
            log.info("WalletRoundLedger doc already exists, returning existing: {}", id);
            return repo.findById(id)
                    .or(() -> {
                        throw new IllegalStateException(
                                "DocumentExistsException but WalletRoundLedger not found: " + id, e);
                    });
        }
    }

    @Override
    public KvDoc<WalletRoundLedger> findById(String id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public void appendTxnRef(String ledgerId, WalletRoundTxnRef ref, BigDecimal vendorLastBalance, long cas) {
        long currentCas = cas;
        int attempts = 0;
        while (true) {
            try {
                repo.appendTxnRef(ledgerId, ref, vendorLastBalance, currentCas);
                return;
            } catch (CasMismatchException e) {
                if (++attempts >= 3) {
                    log.error("appendTxnRef CAS retry exhausted for ledger {}", ledgerId);
                    throw e;
                }
                // Re-fetch to get a fresh CAS, then retry.
                KvDoc<WalletRoundLedger> latest = repo.findById(ledgerId).orElseThrow(() ->
                        new IllegalStateException(
                                "WalletRoundLedger disappeared during appendTxnRef retry: " + ledgerId, e));
                currentCas = latest.getCas();
            }
        }
    }

    @Override
    public void markSettled(String ledgerId) {
        int attempts = 0;
        while (true) {
            KvDoc<WalletRoundLedger> kvDoc = repo.findById(ledgerId).orElseThrow(() ->
                    new IllegalStateException("WalletRoundLedger not found for markSettled: " + ledgerId));
            try {
                repo.markSettled(ledgerId, kvDoc.getCas());
                return;
            } catch (CasMismatchException e) {
                if (++attempts >= 3) {
                    log.error("markSettled CAS retry exhausted for ledger {}", ledgerId);
                    throw e;
                }
            }
        }
    }
}
