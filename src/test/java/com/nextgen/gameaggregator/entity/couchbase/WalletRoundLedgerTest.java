package com.nextgen.gameaggregator.entity.couchbase;

import com.nextgen.gameaggregator.enums.GameRoundState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link WalletRoundLedger} factory + doc-id contract.
 */
class WalletRoundLedgerTest {

    @Test
    void factory_initializesDefaultsAndAuditFields() {
        WalletRoundLedger ledger = WalletRoundLedger.of(
                "jdb",
                "bob",
                "9000123",
                88,
                "SG12Zodiacs",
                "PHP",
                1700000000000L
        );

        assertEquals("jdb", ledger.getVendorClassName());
        assertEquals("bob", ledger.getVendorPlayerUsername());
        assertEquals("9000123", ledger.getVendorRoundId());
        assertEquals(Integer.valueOf(88), ledger.getVendorId());
        assertEquals("SG12Zodiacs", ledger.getVendorGameCode());
        assertEquals("PHP", ledger.getAgentCurrency());
        assertEquals(Long.valueOf(1700000000000L), ledger.getCreatedTs());
        assertNotNull(ledger.getCreatedAt(), "createdAt should be formatted from createdTs");

        // Defaults
        assertEquals(GameRoundState.UNSETTLED, ledger.getState(),
                "fresh ledger starts UNSETTLED");
        assertEquals(Boolean.FALSE, ledger.getIsEnded());
        assertEquals(Integer.valueOf(0), ledger.getTxnCount());
        assertNotNull(ledger.getTransactions());
        assertTrue(ledger.getTransactions().isEmpty(),
                "fresh ledger has no txn refs yet");
    }

    @Test
    void getId_matchesDocIdScheme() {
        WalletRoundLedger ledger = WalletRoundLedger.of(
                "jdb", "bob", "9000123", 88, "SG12Zodiacs", "PHP", 1700000000000L);

        assertEquals("jdb::bob::9000123", ledger.getId(),
                "doc id is vendorClassName::vendorPlayerUsername::vendorRoundId");
        assertEquals(WalletRoundLedger.createDocId("jdb", "bob", "9000123"), ledger.getId(),
                "static createDocId must agree with instance getId");
    }

    @Test
    void newInstance_throughDefaultConstructor_alsoHasDefaults() {
        WalletRoundLedger ledger = new WalletRoundLedger();
        assertEquals(GameRoundState.UNSETTLED, ledger.getState());
        assertEquals(Boolean.FALSE, ledger.getIsEnded());
        assertEquals(Integer.valueOf(0), ledger.getTxnCount());
        assertNotNull(ledger.getTransactions());
    }
}
