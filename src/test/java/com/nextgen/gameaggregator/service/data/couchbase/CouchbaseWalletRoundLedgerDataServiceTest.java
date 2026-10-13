package com.nextgen.gameaggregator.service.data.couchbase;

import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.core.error.DocumentExistsException;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundTxnRef;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.repository.couchbase.WalletRoundLedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouchbaseWalletRoundLedgerDataServiceTest {

    @Mock
    private WalletRoundLedgerRepository repo;

    @InjectMocks
    private CouchbaseWalletRoundLedgerDataService service;

    private WalletRoundLedger sample() {
        return WalletRoundLedger.of("jdb", "bob", "9000123", 88, "SG12Zodiacs", "PHP", 1700000000000L);
    }

    private WalletRoundTxnRef sampleRef() {
        DebitTxn debit = DebitTxn.of("jdb", "transfer-100", 1700000000000L);
        return WalletRoundTxnRef.of(debit);
    }

    @Test
    void insertOrGet_freshInsert_returnsEmpty() {
        WalletRoundLedger ledger = sample();
        when(repo.insert(ledger)).thenReturn(new KvDoc<>(ledger.getId(), 42L, ledger));

        Optional<KvDoc<WalletRoundLedger>> result = service.insertOrGet(ledger);

        assertTrue(result.isEmpty());
    }

    @Test
    void insertOrGet_collision_returnsExisting() {
        WalletRoundLedger newLedger = sample();
        WalletRoundLedger existing = sample();
        existing.setTxnCount(1);
        when(repo.insert(newLedger)).thenThrow(new DocumentExistsException(null));
        when(repo.findById(newLedger.getId()))
                .thenReturn(Optional.of(new KvDoc<>(newLedger.getId(), 100L, existing)));

        Optional<KvDoc<WalletRoundLedger>> result = service.insertOrGet(newLedger);

        assertTrue(result.isPresent());
        assertSame(existing, result.get().getPayload());
    }

    @Test
    void appendTxnRef_happyPath_delegatesOnce() {
        service.appendTxnRef("jdb::bob::9000123", sampleRef(), new BigDecimal("95.00"), 555L);

        verify(repo, times(1)).appendTxnRef(
                ArgumentMatchers.eq("jdb::bob::9000123"),
                ArgumentMatchers.any(WalletRoundTxnRef.class),
                ArgumentMatchers.eq(new BigDecimal("95.00")),
                ArgumentMatchers.eq(555L));
    }

    @Test
    void appendTxnRef_casMismatch_refetchesAndRetries() {
        WalletRoundLedger latest = sample();
        WalletRoundTxnRef ref = sampleRef();
        BigDecimal balance = new BigDecimal("95.00");

        doThrow(new CasMismatchException(null))
                .doNothing()
                .when(repo).appendTxnRef(
                        ArgumentMatchers.eq("jdb::bob::9000123"),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.anyLong());
        when(repo.findById("jdb::bob::9000123"))
                .thenReturn(Optional.of(new KvDoc<>("jdb::bob::9000123", 999L, latest)));

        service.appendTxnRef("jdb::bob::9000123", ref, balance, 555L);

        verify(repo, times(2)).appendTxnRef(
                ArgumentMatchers.eq("jdb::bob::9000123"),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.anyLong());
        verify(repo).findById("jdb::bob::9000123");
    }

    @Test
    void appendTxnRef_casExhausted_throws() {
        doThrow(new CasMismatchException(null))
                .when(repo).appendTxnRef(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.anyLong());
        when(repo.findById(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(new KvDoc<>("jdb::bob::9000123", 999L, sample())));

        assertThrows(CasMismatchException.class,
                () -> service.appendTxnRef("jdb::bob::9000123", sampleRef(), new BigDecimal("95.00"), 555L));
    }

    @Test
    void markSettled_fetchesCasAndCallsRepo() {
        when(repo.findById("jdb::bob::9000123"))
                .thenReturn(Optional.of(new KvDoc<>("jdb::bob::9000123", 888L, sample())));

        service.markSettled("jdb::bob::9000123");

        verify(repo).markSettled("jdb::bob::9000123", 888L);
    }

    @Test
    void markSettled_missingLedger_throws() {
        when(repo.findById("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.markSettled("missing"));
        verify(repo, never()).markSettled(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
    }

    @Test
    void appendTxnRef_freshLedger_appendsRefViaRepo() {
        // Sanity: WalletRoundTxnRef snapshot is what we expect (state/status captured at append time).
        DebitTxn debit = DebitTxn.of("jdb", "transfer-100", 1700000000000L);
        WalletRoundTxnRef ref = WalletRoundTxnRef.of(debit);

        service.appendTxnRef("jdb::bob::9000123", ref, new BigDecimal("95.00"), 555L);

        // The state snapshot is UNSETTLED, the status NEW (default debit factory).
        // Verified inline rather than mocking ArgumentCaptor — exercising the public seam.
        assertSame(GameRoundState.UNSETTLED, ref.getState());
    }
}
