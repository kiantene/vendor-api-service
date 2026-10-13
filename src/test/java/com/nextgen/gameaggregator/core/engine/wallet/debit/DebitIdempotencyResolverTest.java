package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.service.data.DebitTxnDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class DebitIdempotencyResolverTest {

    @Mock private DebitTxnDataService debitTxnDataService;
    @InjectMocks private DebitIdempotencyResolver resolver;

    private DebitContext context() {
        return DebitContext.builder()
                .vendorTxnId("transfer-1")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorRoundId("round-1")
                .vendorId(88)
                .vendorAmount(new BigDecimal("5.00"))
                .vendorTimestamp(1700000000000L)
                .currencyCode("PHP")
                // Deliberately different values per side: a mapping that reads the platform
                // code cannot coincidentally pass.
                .gameCode("JDB_18_18025")
                .vendorGameCode("18_18025")
                // Vendor vs operator twins, again with different values per side.
                .vendorCurrency("PP")
                .agentId(1087)
                .agentPlayerUsername("op-bob")
                .token("ga-token-1")
                .build();
    }

    @Test
    void fresh_recordsTheSnapshotACreditIsResolvedAgainst() {
        when(debitTxnDataService.insertOrGet(ArgumentMatchers.any(DebitTxn.class))).thenReturn(Optional.empty());

        DebitTxn txn = resolver.resolve(context(), 1700000000000L).getTxn();

        AgentMeta agentMeta = txn.getAgentMeta();
        assertNotNull(agentMeta);
        assertEquals(1087, agentMeta.getAgentId());
        assertEquals("op-bob", agentMeta.getUsername(), "the operator-side player, not the vendor uid");
        assertEquals("PHP", agentMeta.getCurrency(), "the operator currency, not the vendor code");
        assertEquals("JDB_18_18025", agentMeta.getGameCode(), "the GA game code the operator was sent");
        assertEquals("ga-token-1", agentMeta.getSession());
        assertEquals("PP", txn.getVendorCurrency(), "the vendor currency the request carried");
    }

    @Test
    void fresh_returnsProceedWithNewTxn() {
        when(debitTxnDataService.insertOrGet(ArgumentMatchers.any(DebitTxn.class))).thenReturn(Optional.empty());

        WalletIdempotencyResolver.Resolution<DebitTxn> res = resolver.resolve(context(), 1700000000000L);

        assertFalse(res.isReplay());
        assertNotNull(res.getTxn());
        assertEquals("transfer-1", res.getTxn().getVendorTxnId());
        assertEquals("bob", res.getTxn().getVendorPlayerUsername());
    }

    @Test
    void persistsTheVendorGameCode_notThePlatformCode() {
        when(debitTxnDataService.insertOrGet(ArgumentMatchers.any(DebitTxn.class))).thenReturn(Optional.empty());

        WalletIdempotencyResolver.Resolution<DebitTxn> res = resolver.resolve(context(), 1700000000000L);

        assertEquals("18_18025", res.getTxn().getVendorGameCode(),
                "DebitTxn.vendorGameCode must hold the vendor-facing code, matching how the legacy "
                        + "writers fill their game-code column (BetContextEnricher / "
                        + "BetResultContextEnricher / AdjustmentContextEnricher all use "
                        + "context.getVendorGameCode()). context.getGameCode() is the platform "
                        + "VendorGame.code and would make records from the two frameworks compare unequal.");
    }

    @Test
    void persistedSuccess_returnsReplay() {
        DebitTxn prior = DebitTxn.of("jdb", "transfer-1", 1L);
        prior.setVendorBalance(new BigDecimal("95.00"));
        prior.setStatus(TxnStatus.SUCCESS);
        when(debitTxnDataService.insertOrGet(ArgumentMatchers.any(DebitTxn.class)))
                .thenReturn(Optional.of(new KvDoc<>(prior.getId(), 4L, prior)));

        WalletIdempotencyResolver.Resolution<DebitTxn> res = resolver.resolve(context(), 1700000000000L);

        assertTrue(res.isReplay());
        assertEquals(0, new BigDecimal("95.00").compareTo(res.getReplayBalance()));
        assertNull(res.getTxn());
    }

    @Test
    void persistedSent_throwsDuplicateRequest() {
        DebitTxn inFlight = DebitTxn.of("jdb", "transfer-1", 1L);
        inFlight.setStatus(TxnStatus.SENT);
        when(debitTxnDataService.insertOrGet(ArgumentMatchers.any(DebitTxn.class)))
                .thenReturn(Optional.of(new KvDoc<>(inFlight.getId(), 3L, inFlight)));

        assertThrows(DuplicateRequestException.class,
                () -> resolver.resolve(context(), 1700000000000L));
    }

    @Test
    void persistedNew_throwsDuplicateRequest() {
        DebitTxn fresh = DebitTxn.of("jdb", "transfer-1", 1L);
        fresh.setStatus(TxnStatus.NEW);
        when(debitTxnDataService.insertOrGet(ArgumentMatchers.any(DebitTxn.class)))
                .thenReturn(Optional.of(new KvDoc<>(fresh.getId(), 3L, fresh)));

        assertThrows(DuplicateRequestException.class,
                () -> resolver.resolve(context(), 1700000000000L));
    }

    @Test
    void persistedError_returnsProceedReusingPersistedDoc() {
        DebitTxn prior = DebitTxn.of("jdb", "transfer-1", 1L);
        prior.setVendorPlayerUsername("bob");
        prior.setVendorRoundId("round-1");
        prior.setStatus(TxnStatus.ERROR);
        when(debitTxnDataService.insertOrGet(ArgumentMatchers.any(DebitTxn.class)))
                .thenReturn(Optional.of(new KvDoc<>(prior.getId(), 9L, prior)));

        WalletIdempotencyResolver.Resolution<DebitTxn> res = resolver.resolve(context(), 1700000000000L);

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
        when(debitTxnDataService.insertOrGet(ArgumentMatchers.any(DebitTxn.class))).thenReturn(Optional.empty());

        WalletIdempotencyResolver.Resolution<DebitTxn> res = resolver.resolve(context(), 1700000000000L);

        assertEquals(WalletIdempotencyResolver.NO_CAS, res.getCas());
    }

    @Test
    void resolve_successWithNullVendorBalance_stillReportsReplay() {
        // markSuccess persists vendorBalance only when non-null, so a SUCCESS doc can carry a
        // null balance. isReplay() must key on the status, not on "balance != null" — otherwise
        // this silently becomes a proceed with a null txn (NPE at markSent, or a second
        // operator call for an already-successful transaction).
        DebitTxn prior = DebitTxn.of("jdb", "transfer-1", 1L);
        prior.setVendorBalance(null);
        prior.setStatus(TxnStatus.SUCCESS);
        when(debitTxnDataService.insertOrGet(ArgumentMatchers.any(DebitTxn.class)))
                .thenReturn(Optional.of(new KvDoc<>(prior.getId(), 4L, prior)));

        WalletIdempotencyResolver.Resolution<DebitTxn> res = resolver.resolve(context(), 1700000000000L);

        assertTrue(res.isReplay());
        assertNull(res.getReplayBalance());
        assertNull(res.getTxn());
    }

    // --- peek: the advisory, wire-fields-only fast path ---

    @Test
    void peek_absent_returnsEmpty() {
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-1")).thenReturn(null);

        assertTrue(resolver.peek(context()).isEmpty());
    }

    @Test
    void peek_success_returnsReplayBalance() {
        DebitTxn prior = DebitTxn.of("jdb", "transfer-1", 1L);
        prior.setVendorBalance(new BigDecimal("95.00"));
        prior.setStatus(TxnStatus.SUCCESS);
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-1"))
                .thenReturn(new KvDoc<>(prior.getId(), 4L, prior));

        WalletIdempotencyResolver.Resolution<DebitTxn> res = resolver.peek(context()).orElseThrow();

        assertTrue(res.isReplay());
        assertEquals(0, new BigDecimal("95.00").compareTo(res.getReplayBalance()));
    }

    @Test
    void peek_new_throwsDuplicateRequest() {
        DebitTxn inFlight = DebitTxn.of("jdb", "transfer-1", 1L);
        inFlight.setStatus(TxnStatus.NEW);
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-1"))
                .thenReturn(new KvDoc<>(inFlight.getId(), 3L, inFlight));

        DebitContext ctx = context();
        assertThrows(DuplicateRequestException.class, () -> resolver.peek(ctx));
    }

    @Test
    void peek_sent_throwsDuplicateRequest() {
        DebitTxn inFlight = DebitTxn.of("jdb", "transfer-1", 1L);
        inFlight.setStatus(TxnStatus.SENT);
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-1"))
                .thenReturn(new KvDoc<>(inFlight.getId(), 3L, inFlight));

        DebitContext ctx = context();
        assertThrows(DuplicateRequestException.class, () -> resolver.peek(ctx));
    }

    @Test
    void peek_error_returnsEmpty() {
        // peek must never re-arm — the caller falls through to resolve(), which owns the claim.
        DebitTxn prior = DebitTxn.of("jdb", "transfer-1", 1L);
        prior.setStatus(TxnStatus.ERROR);
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-1"))
                .thenReturn(new KvDoc<>(prior.getId(), 9L, prior));

        assertTrue(resolver.peek(context()).isEmpty());
    }

    @Test
    void peek_usesWireOnlyDocId_andNeverWrites() {
        // Context carries ONLY the two wire fields the doc id may read. If anyone later reaches
        // for an enriched field (vendorId / gameCode / currencyCode) in docId(), this fails —
        // and peek runs before enrichment, so those would be null in production.
        DebitContext wireOnly = DebitContext.builder()
                .vendorClassName("jdb")
                .vendorTxnId("transfer-1")
                .build();
        ArgumentCaptor<String> docId = ArgumentCaptor.forClass(String.class);
        when(debitTxnDataService.findById(docId.capture())).thenReturn(null);

        assertTrue(resolver.peek(wireOnly).isEmpty());

        assertEquals("jdb::DEBIT::transfer-1", docId.getValue());
        verify(debitTxnDataService, never()).insertOrGet(ArgumentMatchers.any());
    }
}
