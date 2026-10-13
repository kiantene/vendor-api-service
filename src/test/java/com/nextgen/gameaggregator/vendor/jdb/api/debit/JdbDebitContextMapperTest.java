package com.nextgen.gameaggregator.vendor.jdb.api.debit;

import com.nextgen.gameaggregator.core.engine.wallet.debit.DebitContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies JDB Action 9 (debit) → {@link DebitContext} mapping.
 * <p>
 * Two regressions are locked in here; both produced
 * {@code 7501 "User ID cannot be found."} on every live JDB bet:
 * <ol>
 *   <li>{@code systemSessionId} must NOT land on {@code DebitContext.token}. It is JDB's own
 *       session id, never issued or stored by GA, so {@code GameSessionDataService.getGameSession}
 *       took its token branch and threw {@code GameSessionExpiredException} — the
 *       {@code vendorPlayerUsername} branch is unreachable once a token is present.</li>
 *   <li>{@code vendorGameCode} must be the joined {@code gType_mType} form, matching what the
 *       launch persists on the GameSession and what {@code vendor_games.vendor_game_code} holds.
 *       Bare {@code mType} matches no VendorGame row.</li>
 * </ol>
 * Fixtures deliberately differ per side — the expected game code is neither {@code gType} nor
 * {@code mType} alone, and {@code systemSessionId} is a realistic UUID unrelated to any GA token —
 * so a mapping that reads the wrong field cannot coincidentally pass.
 */
class JdbDebitContextMapperTest {

    private final JdbDebitContextMapper mapper = new JdbDebitContextMapper();

    private DebitActionDto baseDto() {
        DebitActionDto dto = new DebitActionDto();
        dto.setAction("9");
        dto.setTs(1789435978413L);
        dto.setTransferId("40562652");
        dto.setUid("tm63tllpc");
        dto.setCurrency("PP");
        dto.setAmount(new BigDecimal("12.00"));
        dto.setGameRoundSeqNo(8000725497L);
        dto.setGType("18");
        dto.setMType("18025");
        dto.setSystemSessionId("64aaeae5-bb58-4e28-8d7f-9ffcb123e45e");
        return dto;
    }

    @Test
    void translates_action9_to_debitContext() {
        DebitContext ctx = mapper.toInternal(baseDto());

        assertEquals("40562652", ctx.getIdempotencyKey());
        assertEquals("40562652", ctx.getVendorTxnId());
        assertEquals("8000725497", ctx.getVendorRoundId());
        assertEquals(new BigDecimal("12.00"), ctx.getVendorAmount());
        assertEquals("tm63tllpc", ctx.getVendorPlayerUsername());
        assertEquals("PP", ctx.getVendorCurrency());
    }

    @Test
    void systemSessionId_isNotMappedOntoToken() {
        DebitContext ctx = mapper.toInternal(baseDto());

        assertNull(ctx.getToken(),
                "systemSessionId is JDB's session id, not a GA token — mapping it onto "
                        + "DebitContext.token makes getGameSession take its throwing token branch "
                        + "and never reach the vendorPlayerUsername lookup (vendor sees 7501). "
                        + "The operator token is backfilled from the resolved session by "
                        + "DebitContextEnricher instead.");
    }

    @Test
    void vendorGameCode_joinsGTypeAndMType() {
        DebitContext ctx = mapper.toInternal(baseDto());

        assertEquals("18_18025", ctx.getVendorGameCode(),
                "GameSession.vendorGameCode and vendor_games.vendor_game_code both store the "
                        + "joined gType_mType form; bare mType (\"18025\") matches no VendorGame row");
    }
}
