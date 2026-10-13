package com.nextgen.gameaggregator.service.data.couchbase;

import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.couchbase.client.core.error.DocumentExistsException;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.repository.couchbase.DebitTxnRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouchbaseDebitTxnDataServiceTest {

    @Mock
    private DebitTxnRepository repo;

    @InjectMocks
    private CouchbaseDebitTxnDataService service;

    private DebitTxn sample() {
        return DebitTxn.of("jdb", "transfer-1", 1700000000000L);
    }

    @Test
    void insertOrGet_freshInsert_returnsEmpty() {
        DebitTxn doc = sample();
        when(repo.insert(doc)).thenReturn(new KvDoc<>(doc.getId(), 42L, doc));

        Optional<KvDoc<DebitTxn>> result = service.insertOrGet(doc);

        assertTrue(result.isEmpty(),
                "fresh insert returns Optional.empty() — load-bearing for Slice 1c idempotency switch");
        verify(repo).insert(doc);
    }

    @Test
    void insertOrGet_onCollision_returnsExisting() {
        DebitTxn newDoc = sample();
        DebitTxn existing = sample();
        existing.setStatus(TxnStatus.SUCCESS);
        when(repo.insert(newDoc)).thenThrow(new DocumentExistsException(null));
        when(repo.findById(newDoc.getId())).thenReturn(Optional.of(new KvDoc<>(newDoc.getId(), 100L, existing)));

        Optional<KvDoc<DebitTxn>> result = service.insertOrGet(newDoc);

        assertTrue(result.isPresent(), "collision must return the existing doc");
        assertSame(existing, result.get().getPayload(),
                "caller switches on existing.getStatus() — needs the persisted doc, not the in-memory one");
    }

    @Test
    void findById_delegatesAndReturnsNullWhenAbsent() {
        when(repo.findById("missing")).thenReturn(Optional.empty());
        assertEquals(null, service.findById("missing"));
    }

    @Test
    void markSent_writesStatusAndSentAt() {
        DebitTxn doc = sample();

        service.markSent(doc, WalletIdempotencyResolver.NO_CAS);

        assertEquals(TxnStatus.SENT, doc.getStatus());
        assertNotNull(doc.getSentAt(), "sentAt timestamp must be stamped");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(repo).update(ArgumentMatchers.eq(doc.getId()), captor.capture(), ArgumentMatchers.eq((Duration) null));
        Map<String, Object> updates = captor.getValue();
        assertEquals("SENT", updates.get("status"));
        assertEquals(doc.getSentAt(), updates.get("sentAt"));
    }

    @Test
    void markSuccess_writesStatusBalanceAndDoneAt() {
        DebitTxn doc = sample();
        BigDecimal balance = new BigDecimal("95.00");

        service.markSuccess(doc, balance);

        assertEquals(TxnStatus.SUCCESS, doc.getStatus());
        assertEquals(balance, doc.getVendorBalance());
        assertNotNull(doc.getDoneAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(repo).update(ArgumentMatchers.eq(doc.getId()), captor.capture(), ArgumentMatchers.eq((Duration) null));
        Map<String, Object> updates = captor.getValue();
        assertEquals("SUCCESS", updates.get("status"));
        assertEquals("95.00", updates.get("vendorBalance"),
                "BigDecimal must be serialized via toPlainString to preserve precision");
        assertEquals(doc.getDoneAt(), updates.get("doneAt"));
    }

    @Test
    void markError_writesStatusExceptionAndDoneAt() {
        DebitTxn doc = sample();

        service.markError(doc, "9999");

        assertEquals(TxnStatus.ERROR, doc.getStatus());
        assertEquals("9999", doc.getException());
        assertNotNull(doc.getDoneAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(repo).update(ArgumentMatchers.eq(doc.getId()), captor.capture(), ArgumentMatchers.eq((Duration) null));
        Map<String, Object> updates = captor.getValue();
        assertEquals("ERROR", updates.get("status"));
        assertEquals("9999", updates.get("exception"));
    }

    @Test
    void markSuccess_doesNotExpireAnOpenDebit() {
        // A SUCCEEDED but unconsumed debit is an open liability — money taken with nothing
        // closing it. Expiring it would delete exactly what unsettled-bet reconciliation looks for.
        DebitTxn doc = new DebitTxn();
        doc.setVendorClassName("jdb");
        doc.setVendorTxnId("transfer-100");

        service.markSuccess(doc, new java.math.BigDecimal("100"));

        verify(repo).update(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap(),
                org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void markError_doesNotExpireTheReArmState() {
        DebitTxn doc = new DebitTxn();
        doc.setVendorClassName("jdb");
        doc.setVendorTxnId("transfer-100");

        service.markError(doc, "TIMEOUT");

        verify(repo).update(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap(),
                org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void casUpdateState_startsTheRetentionClock() {
        // Terminal: consumed by a credit or refund, so the TTL begins here and only here.
        KvDoc<DebitTxn> kvDoc = new KvDoc<>("jdb::DEBIT::transfer-100", 7L, new DebitTxn());

        service.casUpdateState(kvDoc, GameRoundState.SETTLED);

        verify(repo).updateWithCas(
                org.mockito.ArgumentMatchers.eq("jdb::DEBIT::transfer-100"),
                org.mockito.ArgumentMatchers.anyMap(),
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(WalletTxnRetention.TERMINAL_TTL));
    }

    @Test
    void casUpdateState_dispatchesCasUpdate() {
        DebitTxn doc = sample();
        KvDoc<DebitTxn> kvDoc = new KvDoc<>(doc.getId(), 777L, doc);

        service.casUpdateState(kvDoc, GameRoundState.SETTLED);

        verify(repo).updateWithCas(
                ArgumentMatchers.eq(doc.getId()),
                ArgumentMatchers.eq(Map.of("state", "SETTLED")),
                ArgumentMatchers.eq(777L),
                ArgumentMatchers.eq(WalletTxnRetention.TERMINAL_TTL));
    }
}
