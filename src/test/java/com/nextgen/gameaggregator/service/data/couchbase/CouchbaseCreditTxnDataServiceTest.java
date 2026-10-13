package com.nextgen.gameaggregator.service.data.couchbase;

import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.couchbase.client.core.error.DocumentExistsException;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.repository.couchbase.CreditTxnRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouchbaseCreditTxnDataServiceTest {

    @Mock
    private CreditTxnRepository repo;

    @InjectMocks
    private CouchbaseCreditTxnDataService service;

    private CreditTxn settleSample() {
        return CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1700000060000L);
    }

    private CreditTxn refundSample() {
        return CreditTxn.of("jdb", "transfer-300", GameRoundState.REFUNDED, 1700000120000L);
    }

    @Test
    void insertOrGet_freshInsert_returnsEmpty() {
        CreditTxn doc = settleSample();
        when(repo.insert(doc)).thenReturn(new KvDoc<>(doc.getId(), 42L, doc));

        Optional<KvDoc<CreditTxn>> result = service.insertOrGet(doc);

        assertTrue(result.isEmpty());
        verify(repo).insert(doc);
    }

    @Test
    void insertOrGet_onCollision_returnsExisting() {
        CreditTxn newDoc = settleSample();
        CreditTxn existing = settleSample();
        existing.setStatus(TxnStatus.SUCCESS);
        when(repo.insert(newDoc)).thenThrow(new DocumentExistsException(null));
        when(repo.findById(newDoc.getId())).thenReturn(Optional.of(new KvDoc<>(newDoc.getId(), 100L, existing)));

        Optional<KvDoc<CreditTxn>> result = service.insertOrGet(newDoc);

        assertTrue(result.isPresent());
        assertSame(existing, result.get().getPayload());
    }

    @Test
    void markSent_writesStatusAndSentAt_forSettleCredit() {
        CreditTxn doc = settleSample();

        service.markSent(doc, WalletIdempotencyResolver.NO_CAS);

        assertEquals(TxnStatus.SENT, doc.getStatus());
        assertNotNull(doc.getSentAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        // Not terminal: markSent is in flight, markError is the re-arm state — no TTL.
        verify(repo).update(ArgumentMatchers.eq(doc.getId()), captor.capture(),
                ArgumentMatchers.eq((Duration) null));
        assertEquals("SENT", captor.getValue().get("status"));
    }

    @Test
    void markSuccess_writesBalanceAndStatus_forSettleCredit() {
        CreditTxn doc = settleSample();
        BigDecimal balance = new BigDecimal("103.00");

        service.markSuccess(doc, balance);

        assertEquals(TxnStatus.SUCCESS, doc.getStatus());
        assertEquals(balance, doc.getVendorBalance());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        // Terminal: markSuccess starts the retention clock (WalletTxnRetention.TERMINAL_TTL).
        verify(repo).update(ArgumentMatchers.eq(doc.getId()), captor.capture(),
                ArgumentMatchers.eq(WalletTxnRetention.TERMINAL_TTL));
        Map<String, Object> updates = captor.getValue();
        assertEquals("SUCCESS", updates.get("status"));
        assertEquals("103.00", updates.get("vendorBalance"));
    }

    @Test
    void markSuccess_alsoWritesBalance_forRefundCredit() {
        // Settle-mode analytics + refund-mode linkage are written on the initial insertOrGet
        // (the processor's populateAudit step). markSuccess only persists status/balance/doneAt,
        // regardless of whether this is a settle or refund credit. Verify symmetry.
        CreditTxn doc = refundSample();
        BigDecimal balance = new BigDecimal("100.00");

        service.markSuccess(doc, balance);

        assertEquals(TxnStatus.SUCCESS, doc.getStatus());
        assertEquals(balance, doc.getVendorBalance());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        // Terminal: markSuccess starts the retention clock (WalletTxnRetention.TERMINAL_TTL).
        verify(repo).update(ArgumentMatchers.eq(doc.getId()), captor.capture(),
                ArgumentMatchers.eq(WalletTxnRetention.TERMINAL_TTL));
        assertEquals("100.00", captor.getValue().get("vendorBalance"));
    }

    @Test
    void markError_writesExceptionCode() {
        CreditTxn doc = settleSample();

        service.markError(doc, "6006");

        assertEquals(TxnStatus.ERROR, doc.getStatus());
        assertEquals("6006", doc.getException());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        // Not terminal: markSent is in flight, markError is the re-arm state — no TTL.
        verify(repo).update(ArgumentMatchers.eq(doc.getId()), captor.capture(),
                ArgumentMatchers.eq((Duration) null));
        assertEquals("6006", captor.getValue().get("exception"));
    }
}
