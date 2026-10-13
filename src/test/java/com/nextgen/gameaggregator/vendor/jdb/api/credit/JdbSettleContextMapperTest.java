package com.nextgen.gameaggregator.vendor.jdb.api.credit;

import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies JDB Action 10 (settle) → {@link CreditContext} mapping. Critical: the
 * {@code roundClosed} wire field must propagate to {@code isRoundEnded} on the
 * context — that's the framework's signal to flip the ledger to SETTLED at
 * credit-success time.
 */
class JdbSettleContextMapperTest {

    private final JdbSettleContextMapper mapper = new JdbSettleContextMapper();

    private SettleActionDto baseDto() {
        SettleActionDto dto = new SettleActionDto();
        dto.setAction("10");
        dto.setTs(1700000060000L);
        dto.setTransferId("200");
        dto.setUid("bob");
        dto.setCurrency("PHP");
        dto.setAmount(new BigDecimal("8.00"));
        dto.setRefTransferIds(List.of(100L));
        dto.setGameRoundSeqNo(99L);
        dto.setBet(new BigDecimal("5.00"));
        dto.setValidBet(new BigDecimal("5.00"));
        dto.setWin(new BigDecimal("8.00"));
        dto.setNetWin(new BigDecimal("3.00"));
        dto.setHistoryId("hist-7");
        dto.setGType("0");
        dto.setMType("12");
        dto.setSystemSessionId("64aaeae5-bb58-4e28-8d7f-9ffcb123e45e");
        return dto;
    }

    @Test
    void translates_action10_to_settleMode_creditContext() {
        SettleActionDto dto = baseDto();

        CreditContext ctx = mapper.toInternal(dto);

        assertFalse(ctx.isRefund(), "Action 10 must produce isRefund=false context");
        assertEquals("200", ctx.getIdempotencyKey());
        assertEquals("200", ctx.getVendorTxnId());
        assertEquals("99", ctx.getVendorRoundId());
        assertEquals(new BigDecimal("8.00"), ctx.getVendorAmount(),
                "settle: vendorAmount = JDB's `amount` = payout (wallet movement)");
        assertEquals(new BigDecimal("5.00"), ctx.getVendorBetAmount(),
                "settle: vendorBetAmount = JDB's `bet` = original wager (analytics)");
        assertEquals(new BigDecimal("8.00"), ctx.getVendorWinAmount());
        assertEquals(new BigDecimal("3.00"), ctx.getVendorWinLoss(),
                "JDB's wire `netWin` translates to System vocabulary `vendorWinLoss`");
        assertEquals(new BigDecimal("5.00"), ctx.getVendorEffectiveTurnover(),
                "JDB's wire `validBet` translates to System vocabulary `vendorEffectiveTurnover`");
        assertEquals(List.of("100"), ctx.getReferencedVendorDebitTxnIds());
        assertEquals("bob", ctx.getVendorPlayerUsername());
        assertEquals("PHP", ctx.getVendorCurrency());
        assertEquals("0_12", ctx.getVendorGameCode(),
                "GameSession.vendorGameCode / vendor_games.vendor_game_code store the joined "
                        + "gType_mType form; bare mType matches no VendorGame row");
        assertNull(ctx.getToken(),
                "systemSessionId is JDB's session id, not a GA token — see "
                        + "JdbDebitContextMapperTest.systemSessionId_isNotMappedOntoToken");
    }

    @Test
    void roundClosed_true_propagatesToIsRoundEnded() {
        SettleActionDto dto = baseDto();
        dto.setRoundClosed(true);

        CreditContext ctx = mapper.toInternal(dto);

        assertEquals(Boolean.TRUE, ctx.getIsRoundEnded(),
                "roundClosed=true on the wire must flip isRoundEnded to TRUE — drives markSettled");
    }

    @Test
    void roundClosed_false_propagatesAsFalse() {
        SettleActionDto dto = baseDto();
        dto.setRoundClosed(false);

        CreditContext ctx = mapper.toInternal(dto);

        assertEquals(Boolean.FALSE, ctx.getIsRoundEnded(),
                "roundClosed=false (intermediate leg) means do not settle yet");
    }

    @Test
    void roundClosed_null_propagatesAsNull() {
        SettleActionDto dto = baseDto();
        // Don't set roundClosed — JDB may omit on some intermediate legs

        CreditContext ctx = mapper.toInternal(dto);

        assertNull(ctx.getIsRoundEnded(),
                "null roundClosed is preserved; framework treats null as not-yet-settled "
                        + "(markSettled only fires on Boolean.TRUE)");
    }

    @Test
    void betTimeAndSettleTime_bothComeFromTs_notGameDate() {
        // OVI-2164: settle time is ts, same as bet time. A valid gameDate that names a different
        // moment (and would parse as GMT-4) must not win.
        SettleActionDto dto = baseDto();
        dto.setGameDate("22-09-2026 23:17:16");

        CreditContext ctx = mapper.toInternal(dto);

        assertEquals(1700000060000L, ctx.getVendorBetTime());
        assertEquals(1700000060000L, ctx.getVendorSettleTime());
        assertEquals(1700000060000L, dto.getResultTime());
    }

    @Test
    void settleTime_isTs_whenGameDateIsAbsent() {
        SettleActionDto dto = baseDto();
        dto.setGameDate(null);

        assertEquals(1700000060000L, mapper.toInternal(dto).getVendorSettleTime());
    }
}
