package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInSpec;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundTxnRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Locks {@link WalletRoundLedgerRepository#appendTxnRef} behaviour — in particular that a null
 * {@code vendorLastBalance} (the operator-failure path) does NOT abort the mutation: the
 * {@code arrayAppend} + {@code increment} must still land so the failed-credit ref is audited.
 */
@ExtendWith(MockitoExtension.class)
class WalletRoundLedgerRepositoryTest {

    @Mock private Collection collection;
    @Mock private WalletRoundTxnRef ref;

    @SuppressWarnings("unchecked")
    private List<MutateInSpec> captureSpecs() {
        ArgumentCaptor<List<MutateInSpec>> cap = ArgumentCaptor.forClass(List.class);
        verify(collection).mutateIn(eq("ledger-doc"), cap.capture(), any(MutateInOptions.class));
        return cap.getValue();
    }

    @Test
    void appendTxnRef_nullBalance_skipsUpsertButStillAppendsAndIncrements() {
        new WalletRoundLedgerRepository(collection).appendTxnRef("ledger-doc", ref, null, 7L);

        // append(transactions) + increment(txnCount) only — the vendorLastBalance upsert is skipped
        // rather than NPE on null.toPlainString(), so the ref still lands on the failure path.
        assertEquals(2, captureSpecs().size());
    }

    @Test
    void appendTxnRef_withBalance_includesVendorLastBalanceUpsert() {
        new WalletRoundLedgerRepository(collection)
                .appendTxnRef("ledger-doc", ref, new BigDecimal("106.50"), 7L);

        // append + increment + upsert(vendorLastBalance)
        assertEquals(3, captureSpecs().size());
    }
}
