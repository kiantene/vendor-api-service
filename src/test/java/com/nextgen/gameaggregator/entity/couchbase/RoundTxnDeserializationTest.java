package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the polymorphism contract for the {@link WalletTxn} hierarchy
 * (the debit/credit family). Independent polymorphic root with its own
 * {@code @JsonTypeInfo} on {@code kind}; resolves to {@link DebitTxn} or
 * {@link CreditTxn} per the {@code kind} value. Zero coupling to {@link RoundTxn}.
 */
class RoundTxnDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void newDocWithKindDebit_deserializesAsDebitTxn() throws Exception {
        String json = """
                {
                  "kind": "DEBIT",
                  "state": "UNSETTLED",
                  "status": "SUCCESS",
                  "gaTxnId": "01HXYZ-debit",
                  "vendorTxnId": "transfer-100",
                  "vendorClassName": "jdb",
                  "vendorId": 88,
                  "vendorRoundId": "jdb-round-1",
                  "vendorPlayerUsername": "bob",
                  "vendorGameCode": "jdb-slot01",
                  "agentCurrency": "PHP",
                  "vendorAmount": "5.50",
                  "vendorBalance": "94.50",
                  "vendorBetTime": 1700000000000,
                  "createdTs": 1700000000000
                }
                """;

        WalletTxn deserialized = mapper.readValue(json, WalletTxn.class);

        assertInstanceOf(DebitTxn.class, deserialized);
        DebitTxn debit = (DebitTxn) deserialized;
        assertEquals(GameRoundState.UNSETTLED, debit.getState());
        assertEquals(TxnStatus.SUCCESS, debit.getStatus());
        assertEquals("transfer-100", debit.getVendorTxnId());
        assertEquals("01HXYZ-debit", debit.getGaTxnId());
        assertEquals(new BigDecimal("5.50"), debit.getVendorAmount());
        assertEquals(new BigDecimal("94.50"), debit.getVendorBalance());
    }

    @Test
    void newDocWithKindCreditSettleMode_deserializesAsCreditTxn() throws Exception {
        String json = """
                {
                  "kind": "CREDIT",
                  "state": "SETTLED",
                  "status": "SUCCESS",
                  "gaTxnId": "01HXYZ-credit",
                  "vendorTxnId": "transfer-200",
                  "vendorClassName": "jdb",
                  "vendorId": 88,
                  "vendorRoundId": "jdb-round-1",
                  "vendorPlayerUsername": "bob",
                  "vendorAmount": "12.00",
                  "vendorBetAmount": "5.50",
                  "vendorWinAmount": "12.00",
                  "vendorWinLoss": "6.50",
                  "vendorEffectiveTurnover": "5.50",
                  "vendorBalance": "106.50",
                  "isRoundEnded": true,
                  "vendorBetTime": 1700000000000,
                  "vendorSettleTime": 1700000060000,
                  "createdTs": 1700000060000
                }
                """;

        WalletTxn deserialized = mapper.readValue(json, WalletTxn.class);

        assertInstanceOf(CreditTxn.class, deserialized);
        CreditTxn credit = (CreditTxn) deserialized;
        assertEquals(GameRoundState.SETTLED, credit.getState());
        assertEquals(new BigDecimal("12.00"), credit.getVendorAmount(),
                "settle: vendorAmount = payout (wallet movement)");
        assertEquals(new BigDecimal("5.50"), credit.getVendorBetAmount(),
                "settle: vendorBetAmount = original wager (analytics passthrough)");
        assertEquals(new BigDecimal("12.00"), credit.getVendorWinAmount());
        assertEquals(new BigDecimal("6.50"), credit.getVendorWinLoss());
        assertEquals(Boolean.TRUE, credit.getIsRoundEnded());
        assertTrue(credit.getReferencedVendorDebitTxnIds() == null || credit.getReferencedVendorDebitTxnIds().isEmpty(),
                "settle-mode credit should not carry refund linkage");
    }

    @Test
    void newDocWithKindCreditRefundMode_deserializesAsCreditTxn() throws Exception {
        String json = """
                {
                  "kind": "CREDIT",
                  "state": "REFUNDED",
                  "status": "SUCCESS",
                  "gaTxnId": "01HXYZ-refund",
                  "vendorTxnId": "transfer-300",
                  "vendorClassName": "jdb",
                  "vendorId": 88,
                  "vendorRoundId": "jdb-round-1",
                  "vendorPlayerUsername": "bob",
                  "vendorAmount": "5.50",
                  "vendorBalance": "100.00",
                  "referencedVendorDebitTxnIds": ["transfer-100"],
                  "referencedDebitGaTxnIds": ["01HXYZ-debit"],
                  "createdTs": 1700000120000
                }
                """;

        WalletTxn deserialized = mapper.readValue(json, WalletTxn.class);

        assertInstanceOf(CreditTxn.class, deserialized);
        CreditTxn credit = (CreditTxn) deserialized;
        assertEquals(GameRoundState.REFUNDED, credit.getState());
        assertTrue(credit.isRefund());
        assertEquals(List.of("transfer-100"), credit.getReferencedVendorDebitTxnIds());
        assertEquals(List.of("01HXYZ-debit"), credit.getReferencedDebitGaTxnIds());
    }

    @Test
    void debitTxnRoundTrip_preservesKindAndOmitsLegacyBetFields() throws Exception {
        DebitTxn original = DebitTxn.of("jdb", "transfer-rt", 1700000000000L);
        original.setVendorId(88);
        original.setVendorRoundId("jdb-rt-round");
        original.setVendorPlayerUsername("carol");
        original.setVendorAmount(new BigDecimal("7.25"));
        original.setVendorBalance(new BigDecimal("92.75"));
        original.setVendorBetTime(1700000000000L);
        original.setStatus(TxnStatus.SUCCESS);

        String json = mapper.writeValueAsString(original);
        assertTrue(json.contains("\"kind\":\"DEBIT\""), "serialized DebitTxn must carry kind=DEBIT");
        assertTrue(!json.contains("\"vendorBetId\""),
                "WalletTxn hierarchy must not carry legacy BET vendorBetId field");
        assertTrue(!json.contains("\"gaBetId\""),
                "WalletTxn hierarchy must not carry legacy BET gaBetId field");

        WalletTxn roundTripped = mapper.readValue(json, WalletTxn.class);
        assertInstanceOf(DebitTxn.class, roundTripped);
        DebitTxn d = (DebitTxn) roundTripped;
        assertEquals("transfer-rt", d.getVendorTxnId());
        assertEquals(new BigDecimal("7.25"), d.getVendorAmount());
        assertEquals(TxnStatus.SUCCESS, d.getStatus());
    }

    @Test
    void creditTxnFactory_rejectsInvalidState() {
        try {
            CreditTxn.of("jdb", "transfer-bad", GameRoundState.UNSETTLED, 1700000000000L);
            throw new AssertionError("Expected IllegalArgumentException for non-terminal state");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("SETTLED or REFUNDED"),
                    "exception message should explain the valid states");
        }
    }
}
