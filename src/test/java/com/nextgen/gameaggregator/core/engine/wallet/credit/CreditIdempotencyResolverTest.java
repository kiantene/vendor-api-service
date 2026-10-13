package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.service.data.CreditTxnDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditIdempotencyResolverTest {

    @Mock private CreditTxnDataService creditTxnDataService;
    @InjectMocks private CreditIdempotencyResolver resolver;

    private CreditContext settleContext() {
        return CreditContext.builder()
                .vendorTxnId("transfer-200")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorRoundId("round-1")
                .vendorId(88)
                .vendorAmount(new BigDecimal("12.00"))
                .vendorBetAmount(new BigDecimal("5.50"))
                .vendorWinAmount(new BigDecimal("12.00"))
                .currencyCode("PHP")
                .vendorCurrency("PHP")
                .gameCode("JDB_18_18025")
                .vendorGameCode("18_18025")
                .vendorBetTime(1700000000000L)
                .vendorSettleTime(1700000060000L)
                .isRoundEnded(true)
                .isRefund(false)
                .build();
    }

    private CreditContext refundContext() {
        return CreditContext.builder()
                .vendorTxnId("transfer-300")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorRoundId("round-1")
                .vendorId(88)
                .vendorAmount(new BigDecimal("5.50"))
                .currencyCode("PHP")
                .vendorCurrency("PHP")
                .gameCode("JDB_18_18025")
                .vendorGameCode("18_18025")
                .referencedVendorDebitTxnIds(List.of("transfer-100"))
                .isRefund(true)
                .build();
    }

    @Test
    void fresh_returnsProceedWithNewTxn() {
        when(creditTxnDataService.insertOrGet(ArgumentMatchers.any(CreditTxn.class))).thenReturn(Optional.empty());
        WalletIdempotencyResolver.Resolution<CreditTxn> res = resolver.resolve(settleContext(), 1700000000000L);

        assertFalse(res.isReplay());
        assertNotNull(res.getTxn());
        assertEquals("transfer-200", res.getTxn().getVendorTxnId());
        assertEquals(GameRoundState.SETTLED, res.getTxn().getState());
    }

    @Test
    void freshRefund_returnsProceedWithRefundedState() {
        when(creditTxnDataService.insertOrGet(ArgumentMatchers.any(CreditTxn.class))).thenReturn(Optional.empty());
        WalletIdempotencyResolver.Resolution<CreditTxn> res = resolver.resolve(refundContext(), 1700000000000L);

        assertFalse(res.isReplay());
        assertEquals(GameRoundState.REFUNDED, res.getTxn().getState());
        assertEquals(List.of("transfer-100"), res.getTxn().getReferencedVendorDebitTxnIds());
    }

    @Test
    void persistsTheVendorGameCode_notThePlatformCode() {
        when(creditTxnDataService.insertOrGet(ArgumentMatchers.any(CreditTxn.class))).thenReturn(Optional.empty());

        WalletIdempotencyResolver.Resolution<CreditTxn> res = resolver.resolve(settleContext(), 1700000000000L);

        assertEquals("18_18025", res.getTxn().getVendorGameCode(),
                "CreditTxn.vendorGameCode must hold the vendor-facing code — see "
                        + "DebitIdempotencyResolverTest.persistsTheVendorGameCode_notThePlatformCode");
    }

    @Test
    void persistedSuccess_returnsReplayBalance() {
        CreditTxn priorSuccess = CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1L);
        priorSuccess.setVendorBalance(new BigDecimal("106.50"));
        priorSuccess.setStatus(TxnStatus.SUCCESS);
        when(creditTxnDataService.insertOrGet(ArgumentMatchers.any(CreditTxn.class)))
                .thenReturn(Optional.of(new KvDoc<>(priorSuccess.getId(), 4L, priorSuccess)));

        WalletIdempotencyResolver.Resolution<CreditTxn> res = resolver.resolve(settleContext(), 1700000000000L);

        assertTrue(res.isReplay());
        assertEquals(0, new BigDecimal("106.50").compareTo(res.getReplayBalance()));
        assertNull(res.getTxn());
    }

    @Test
    void persistedSent_throwsDuplicateRequest() {
        CreditTxn inFlight = CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1L);
        inFlight.setStatus(TxnStatus.SENT);
        when(creditTxnDataService.insertOrGet(ArgumentMatchers.any(CreditTxn.class)))
                .thenReturn(Optional.of(new KvDoc<>(inFlight.getId(), 3L, inFlight)));

        assertThrows(DuplicateRequestException.class, () -> resolver.resolve(settleContext(), 1700000000000L));
    }

    @Test
    void persistedNew_throwsDuplicateRequest() {
        CreditTxn fresh = CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1L);
        fresh.setStatus(TxnStatus.NEW);
        when(creditTxnDataService.insertOrGet(ArgumentMatchers.any(CreditTxn.class)))
                .thenReturn(Optional.of(new KvDoc<>(fresh.getId(), 3L, fresh)));

        assertThrows(DuplicateRequestException.class, () -> resolver.resolve(settleContext(), 1700000000000L));
    }

    @Test
    void persistedError_returnsProceedReusingPersistedDoc() {
        CreditTxn prior = CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1L);
        prior.setStatus(TxnStatus.ERROR);
        prior.setVendorPlayerUsername("bob");
        prior.setVendorRoundId("round-1");
        when(creditTxnDataService.insertOrGet(ArgumentMatchers.any(CreditTxn.class)))
                .thenReturn(Optional.of(new KvDoc<>(prior.getId(), 9L, prior)));

        WalletIdempotencyResolver.Resolution<CreditTxn> res = resolver.resolve(settleContext(), 1700000000000L);

        assertFalse(res.isReplay());
        assertEquals(prior.getGaTxnId(), res.getTxn().getGaTxnId(),
                "re-arm reuses the persisted txn (same gaTxnId)");
        assertEquals(9L, res.getCas(),
                "the persisted doc's CAS travels with the re-arm so markSent can claim it exclusively");
    }

    @Test
    void fresh_carriesNoCas() {
        // A fresh insert needs no CAS guard: this caller just won the atomic insert, so no other
        // thread holds the document. Only the ERROR re-arm races.
        when(creditTxnDataService.insertOrGet(ArgumentMatchers.any(CreditTxn.class))).thenReturn(Optional.empty());

        WalletIdempotencyResolver.Resolution<CreditTxn> res = resolver.resolve(settleContext(), 1700000000000L);

        assertEquals(WalletIdempotencyResolver.NO_CAS, res.getCas());
    }

    @Test
    void resolve_successWithNullVendorBalance_stillReportsReplay() {
        // markSuccess persists vendorBalance only when non-null, so a SUCCESS doc can carry a
        // null balance. isReplay() must key on the status, not on "balance != null" — otherwise
        // this silently becomes a proceed with a null txn (NPE at markSent, or a second
        // operator call for an already-successful transaction).
        CreditTxn priorSuccess = CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1L);
        priorSuccess.setVendorBalance(null);
        priorSuccess.setStatus(TxnStatus.SUCCESS);
        when(creditTxnDataService.insertOrGet(ArgumentMatchers.any(CreditTxn.class)))
                .thenReturn(Optional.of(new KvDoc<>(priorSuccess.getId(), 4L, priorSuccess)));

        WalletIdempotencyResolver.Resolution<CreditTxn> res = resolver.resolve(settleContext(), 1700000000000L);

        assertTrue(res.isReplay());
        assertNull(res.getReplayBalance());
        assertNull(res.getTxn());
    }

    // --- peek: the advisory, wire-fields-only fast path ---

    @Test
    void peek_absent_returnsEmpty() {
        when(creditTxnDataService.findById("jdb::CREDIT::transfer-200")).thenReturn(null);

        assertTrue(resolver.peek(settleContext()).isEmpty());
    }

    @Test
    void peek_success_returnsReplayBalance() {
        CreditTxn priorSuccess = CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1L);
        priorSuccess.setVendorBalance(new BigDecimal("106.50"));
        priorSuccess.setStatus(TxnStatus.SUCCESS);
        when(creditTxnDataService.findById("jdb::CREDIT::transfer-200"))
                .thenReturn(new KvDoc<>(priorSuccess.getId(), 4L, priorSuccess));

        WalletIdempotencyResolver.Resolution<CreditTxn> res = resolver.peek(settleContext()).orElseThrow();

        assertTrue(res.isReplay());
        assertEquals(0, new BigDecimal("106.50").compareTo(res.getReplayBalance()));
    }

    @Test
    void peek_new_throwsDuplicateRequest() {
        CreditTxn inFlight = CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1L);
        inFlight.setStatus(TxnStatus.NEW);
        when(creditTxnDataService.findById("jdb::CREDIT::transfer-200"))
                .thenReturn(new KvDoc<>(inFlight.getId(), 3L, inFlight));

        CreditContext ctx = settleContext();
        assertThrows(DuplicateRequestException.class, () -> resolver.peek(ctx));
    }

    @Test
    void peek_sent_throwsDuplicateRequest() {
        CreditTxn inFlight = CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1L);
        inFlight.setStatus(TxnStatus.SENT);
        when(creditTxnDataService.findById("jdb::CREDIT::transfer-200"))
                .thenReturn(new KvDoc<>(inFlight.getId(), 3L, inFlight));

        CreditContext ctx = settleContext();
        assertThrows(DuplicateRequestException.class, () -> resolver.peek(ctx));
    }

    @Test
    void peek_error_returnsEmpty() {
        // peek must never re-arm — the caller falls through to resolve(), which owns the claim.
        CreditTxn prior = CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1L);
        prior.setStatus(TxnStatus.ERROR);
        when(creditTxnDataService.findById("jdb::CREDIT::transfer-200"))
                .thenReturn(new KvDoc<>(prior.getId(), 9L, prior));

        assertTrue(resolver.peek(settleContext()).isEmpty());
    }

    @Test
    void peek_usesWireOnlyDocId_andNeverWrites() {
        // Context carries ONLY the two wire fields the doc id may read. If anyone later reaches
        // for an enriched field (vendorId / gameCode / currencyCode) in docId(), this fails —
        // and peek runs before enrichment, so those would be null in production.
        CreditContext wireOnly = CreditContext.builder()
                .vendorClassName("jdb")
                .vendorTxnId("transfer-200")
                .build();
        ArgumentCaptor<String> docId = ArgumentCaptor.forClass(String.class);
        when(creditTxnDataService.findById(docId.capture())).thenReturn(null);

        assertTrue(resolver.peek(wireOnly).isEmpty());

        assertEquals("jdb::CREDIT::transfer-200", docId.getValue());
        verify(creditTxnDataService, never()).insertOrGet(ArgumentMatchers.any());
    }
}
