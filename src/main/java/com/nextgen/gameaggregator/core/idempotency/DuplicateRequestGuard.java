package com.nextgen.gameaggregator.core.idempotency;

import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import org.springframework.stereotype.Service;

@Service
public class DuplicateRequestGuard {
    private static final ThreadLocal<GameTransaction> currentRequest = new ThreadLocal<>();
    private final RequestIdempotencyService service;
    private final GameTransactionService txnService;

    public DuplicateRequestGuard(RequestIdempotencyService service,
                                 GameTransactionService txnService) {
        this.service = service;
        this.txnService = txnService;
    }

    public void ensureNotDuplicate(String vendor, String action, String key) {
        if (service.isDuplicateRequest(vendor, action, key)) {
            throw new DuplicateRequestException(RequestIdempotencyService.key(vendor, action, key) + " already processed");
        }
    }

    public GameTransaction ensureNotDuplicate(TxnType type, String vendorClassName, String key) {
        GameTransaction probe = GameTransaction.of(type, vendorClassName, key);
        var doc = txnService.get(probe);
        if (doc.isPresent()) {
            GameTransaction txn = doc.get();
            throw new DuplicateRequestException(txn.getId() + " is already processed", txn);
        }
        currentRequest.set(txnService.save(probe));
        return probe;
    }

    public void clear() {
        GameTransaction txn = currentRequest.get();
        if (txn != null) {
            txnService.deleteById(txn.getId());
            currentRequest.remove();
        }
        service.clearCurrentRequest();
    }

    public void cleanup() {
        currentRequest.remove();
        RequestIdempotencyService.cleanupThreadLocal();
    }
}
