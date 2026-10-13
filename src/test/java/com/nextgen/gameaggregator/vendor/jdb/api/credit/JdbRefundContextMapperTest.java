package com.nextgen.gameaggregator.vendor.jdb.api.credit;

import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the wire→platform translation for JDB Action 11 (cancel).
 * Ensures {@code isRefund=true}, {@code referencedVendorDebitTxnIds} is populated from
 * {@code refTransferIds}, and JDB-specific taxonomy doesn't pollute the context.
 */
class JdbRefundContextMapperTest {

    private final JdbRefundContextMapper mapper = new JdbRefundContextMapper();

    @Test
    void translates_action11_to_refundMode_creditContext() {
        RefundActionDto dto = new RefundActionDto();
        dto.setAction("11");
        dto.setTs(1700000000000L);
        dto.setTransferId("300");
        dto.setUid("bob");
        dto.setCurrency("PHP");
        dto.setAmount(new BigDecimal("5.50"));
        dto.setRefTransferIds(List.of(100L, 200L));
        dto.setGameRoundSeqNo(99L);
        dto.setGType("3");
        dto.setMType("12");
        dto.setSystemSessionId("64aaeae5-bb58-4e28-8d7f-9ffcb123e45e");

        CreditContext ctx = mapper.toInternal(dto);

        assertTrue(ctx.isRefund(), "Action 11 must produce isRefund=true context");
        assertEquals("300", ctx.getIdempotencyKey());
        assertEquals("300", ctx.getVendorTxnId());
        assertEquals("99", ctx.getVendorRoundId());
        assertEquals(new BigDecimal("5.50"), ctx.getVendorAmount(),
                "refund: vendorAmount = JDB's refund value (wallet movement)");
        assertEquals(null, ctx.getVendorBetAmount(),
                "refund: vendorBetAmount is not set by JDB Action 11 (no separate `bet` field on the wire)");
        assertEquals(1700000000000L, ctx.getVendorTimestamp());
        assertEquals(List.of("100", "200"), ctx.getReferencedVendorDebitTxnIds(),
                "refTransferIds must translate to platform-named referencedVendorDebitTxnIds as strings");
        assertEquals("bob", ctx.getVendorPlayerUsername());
        assertEquals("PHP", ctx.getVendorCurrency());
        assertEquals("3_12", ctx.getVendorGameCode(),
                "GameSession.vendorGameCode / vendor_games.vendor_game_code store the joined "
                        + "gType_mType form; bare mType matches no VendorGame row");
        assertEquals(null, ctx.getToken(),
                "systemSessionId is JDB's session id, not a GA token — see "
                        + "JdbDebitContextMapperTest.systemSessionId_isNotMappedOntoToken");

        // Settle-mode analytics fields must stay null in refund mode
        assertEquals(null, ctx.getVendorWinAmount());
        assertEquals(null, ctx.getVendorEffectiveTurnover());
        assertEquals(null, ctx.getIsRoundEnded());
    }
}
