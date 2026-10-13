package com.nextgen.gameaggregator.entity.couchbase;

import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link WalletRoundTxnRef#of(DebitTxn)} / {@link WalletRoundTxnRef#of(CreditTxn)}
 * snapshot factories. Verifies the 5 fields are captured correctly and the kind discriminator
 * matches the {@link WalletTxn} {@code @JsonTypeInfo} string.
 */
class WalletRoundTxnRefTest {

    @Test
    void of_debitTxn_capturesSnapshotFields() {
        DebitTxn debit = DebitTxn.of("jdb", "transfer-100", 1700000000000L);
        debit.setStatus(TxnStatus.SUCCESS);

        WalletRoundTxnRef ref = WalletRoundTxnRef.of(debit);

        assertEquals("jdb::DEBIT::transfer-100", ref.getDocId(),
                "docId points to the standalone DebitTxn doc");
        assertEquals("DEBIT", ref.getKind(),
                "kind string matches WalletTxn @JsonTypeInfo discriminator");
        assertNotNull(ref.getGaTxnId(), "gaTxnId carried over from the debit factory");
        assertEquals(debit.getGaTxnId(), ref.getGaTxnId());
        assertEquals(GameRoundState.UNSETTLED, ref.getState(),
                "debit starts UNSETTLED — snapshot captures that");
        assertEquals(TxnStatus.SUCCESS, ref.getStatus(),
                "status snapshot captures the value at append time");
    }

    @Test
    void of_creditTxn_settleMode_capturesSnapshot() {
        CreditTxn credit = CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1700000060000L);
        credit.setStatus(TxnStatus.SUCCESS);

        WalletRoundTxnRef ref = WalletRoundTxnRef.of(credit);

        assertEquals("jdb::CREDIT::transfer-200", ref.getDocId());
        assertEquals("CREDIT", ref.getKind());
        assertEquals(credit.getGaTxnId(), ref.getGaTxnId());
        assertEquals(GameRoundState.SETTLED, ref.getState(),
                "settle-mode credit's state is SETTLED-at-creation");
        assertEquals(TxnStatus.SUCCESS, ref.getStatus());
    }

    @Test
    void of_creditTxn_refundMode_capturesSnapshot() {
        CreditTxn credit = CreditTxn.of("jdb", "transfer-300", GameRoundState.REFUNDED, 1700000120000L);
        credit.setStatus(TxnStatus.SUCCESS);

        WalletRoundTxnRef ref = WalletRoundTxnRef.of(credit);

        assertEquals("jdb::CREDIT::transfer-300", ref.getDocId());
        assertEquals("CREDIT", ref.getKind(),
                "refund is a CREDIT in this framework — refund vs settle is on the state, not the kind");
        assertEquals(GameRoundState.REFUNDED, ref.getState());
    }

    @Test
    void of_capturesErrorStatusSnapshot() {
        DebitTxn debit = DebitTxn.of("jdb", "transfer-err", 1700000000000L);
        debit.setStatus(TxnStatus.ERROR);

        WalletRoundTxnRef ref = WalletRoundTxnRef.of(debit);

        assertEquals(TxnStatus.ERROR, ref.getStatus(),
                "snapshot-at-append: ERROR refs land in the ledger for reconciliation tooling");
    }
}
