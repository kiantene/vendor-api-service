package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundTxnRef;
import com.nextgen.gameaggregator.service.data.WalletRoundLedgerDataService;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.WalletTransactionHistoryProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebitAuditPublisherTest {

    @Mock private WalletRoundLedgerDataService walletRoundLedgerDataService;
    @Mock private WalletTransactionHistoryProducer walletTransactionHistoryProducer;
    @Mock private VendorConfigService vendorConfigService;

    @InjectMocks private DebitAuditPublisher publisher;

    private DebitContext context() {
        return DebitContext.builder()
                .vendorTxnId("transfer-1")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorRoundId("round-1")
                .build();
    }

    private DebitTxn txn() {
        DebitTxn t = DebitTxn.of("jdb", "transfer-1", 1L);
        t.setVendorPlayerUsername("bob");
        t.setVendorRoundId("round-1");
        return t;
    }

    @Test
    void appendsTxnRefAndEmitsTxnHistory() {
        KvDoc<WalletRoundLedger> ledgerKv = new KvDoc<>("jdb::bob::round-1", 42L, new WalletRoundLedger());
        when(vendorConfigService.isTransactionHistoryEnabled("jdb")).thenReturn(true);

        publisher.publishSuccess(context(), txn(), new BigDecimal("95.00"), ledgerKv);

        ArgumentCaptor<WalletRoundTxnRef> refCap = ArgumentCaptor.forClass(WalletRoundTxnRef.class);
        verify(walletRoundLedgerDataService).appendTxnRef(
                ArgumentMatchers.eq("jdb::bob::round-1"),
                refCap.capture(),
                argThat(b -> b != null && b.compareTo(new BigDecimal("95.00")) == 0),
                ArgumentMatchers.eq(42L));
        assertEquals("DEBIT", refCap.getValue().getKind());
        verify(walletTransactionHistoryProducer).publishTransactionHistoryForDebit(
                ArgumentMatchers.any(DebitContext.class), ArgumentMatchers.any(DebitTxn.class));
    }

    @Test
    void txnHistoryDisabled_skipsKafkaEmit() {
        KvDoc<WalletRoundLedger> ledgerKv = new KvDoc<>("jdb::bob::round-1", 42L, new WalletRoundLedger());
        when(vendorConfigService.isTransactionHistoryEnabled("jdb")).thenReturn(false);

        publisher.publishSuccess(context(), txn(), new BigDecimal("95.00"), ledgerKv);

        verify(walletRoundLedgerDataService).appendTxnRef(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(walletTransactionHistoryProducer, never()).publishTransactionHistoryForDebit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void kafkaEmitFails_doesNotMaskAppendSuccess() {
        KvDoc<WalletRoundLedger> ledgerKv = new KvDoc<>("jdb::bob::round-1", 42L, new WalletRoundLedger());
        when(vendorConfigService.isTransactionHistoryEnabled("jdb")).thenReturn(true);
        doThrow(new RuntimeException("kafka unreachable"))
                .when(walletTransactionHistoryProducer)
                .publishTransactionHistoryForDebit(
                        ArgumentMatchers.any(DebitContext.class),
                        ArgumentMatchers.any(DebitTxn.class));

        assertDoesNotThrow(() -> publisher.publishSuccess(
                context(), txn(), new BigDecimal("95.00"), ledgerKv));
    }

    @Test
    void appendTxnRefFails_doesNotPropagateToVendor() {
        // The operator wallet is already debited by the time audit runs. A ledger-ref write failure
        // must be swallowed (logged), not surfaced to the vendor as a false failure — and execution
        // must still reach the txn-history step afterwards.
        KvDoc<WalletRoundLedger> ledgerKv = new KvDoc<>("jdb::bob::round-1", 42L, new WalletRoundLedger());
        doThrow(new RuntimeException("couchbase unreachable"))
                .when(walletRoundLedgerDataService)
                .appendTxnRef(ArgumentMatchers.anyString(), ArgumentMatchers.any(),
                        ArgumentMatchers.any(), ArgumentMatchers.anyLong());

        assertDoesNotThrow(() -> publisher.publishSuccess(
                context(), txn(), new BigDecimal("95.00"), ledgerKv));

        verify(vendorConfigService).isTransactionHistoryEnabled("jdb");
    }
}
