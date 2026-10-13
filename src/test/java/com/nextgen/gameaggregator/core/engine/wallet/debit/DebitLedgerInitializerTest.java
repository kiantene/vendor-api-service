package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.service.data.WalletRoundLedgerDataService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebitLedgerInitializerTest {

    @Mock private WalletRoundLedgerDataService walletRoundLedgerDataService;
    @InjectMocks private DebitLedgerInitializer initializer;

    private DebitContext context() {
        return DebitContext.builder()
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorRoundId("round-1")
                .vendorId(88)
                .gameCode("JDB_18_18025")
                .vendorGameCode("18_18025")
                .currencyCode("PHP")
                .vendorAmount(new BigDecimal("5.00"))
                .build();
    }

    @Test
    void freshInsert_returnsFindByIdResult() {
        WalletRoundLedger fresh = new WalletRoundLedger();
        when(walletRoundLedgerDataService.insertOrGet(ArgumentMatchers.any(WalletRoundLedger.class)))
                .thenReturn(Optional.empty());
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 42L, fresh));

        KvDoc<WalletRoundLedger> result = initializer.ensure(context(), 1700000000000L);

        assertEquals("jdb::bob::round-1", result.getId());
        assertEquals(42L, result.getCas());
    }

    @Test
    void ledgerCarriesTheVendorGameCode_notThePlatformCode() {
        when(walletRoundLedgerDataService.insertOrGet(ArgumentMatchers.any(WalletRoundLedger.class)))
                .thenReturn(Optional.empty());
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 1L, new WalletRoundLedger()));

        initializer.ensure(context(), 1700000000000L);

        ArgumentCaptor<WalletRoundLedger> captor = ArgumentCaptor.forClass(WalletRoundLedger.class);
        verify(walletRoundLedgerDataService).insertOrGet(captor.capture());
        assertEquals("18_18025", captor.getValue().getVendorGameCode(),
                "the ledger must agree with its own transactions — WalletRoundLedger.of takes "
                        + "vendorGameCode in that position, so passing context.getGameCode() stored "
                        + "the platform VendorGame.code instead");
    }

    @Test
    void existingLedger_returnsExistingWithoutFindById() {
        WalletRoundLedger existing = new WalletRoundLedger();
        KvDoc<WalletRoundLedger> existingKv = new KvDoc<>("jdb::bob::round-1", 7L, existing);
        when(walletRoundLedgerDataService.insertOrGet(ArgumentMatchers.any(WalletRoundLedger.class)))
                .thenReturn(Optional.of(existingKv));

        KvDoc<WalletRoundLedger> result = initializer.ensure(context(), 1700000000000L);

        assertEquals(7L, result.getCas());
        verify(walletRoundLedgerDataService, never()).findById(ArgumentMatchers.anyString());
    }
}
