package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.context.VendorPlayerAware;
import com.nextgen.gameaggregator.core.engine.game.GameSessionData;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Field mapping rules for Bet Request:
 * <p></p>
 * <p>idempotencyKey - Unique key to stop duplicate transactions.</p>
 * <p>vendorBetId    - Vendor’s bet ID (if provided in API doc). Default to same value as `idempotencyKey` if not mapped</p>
 * <p>roundId        - Vendor’s round ID, used for rollback when rollbackType = BY_ROUND.</p>
 */
@Data
@Builder
public class BetContext implements GameSessionData, VendorPlayerAware {
    /**
     * Provided by the vendor to ensure the same bet request is not processed multiple times.
     */
    private String idempotencyKey;
    /**
     * The unique identifier for this specific bet as provided by the vendor.
     * Used for reconciliation and tracking with the external system.
     */
    private String vendorBetId;
    /**
     * The unique identifier for the current game round.
     * This helps group related transactions within a single game round.
     */
    private String roundId;
    private String vendorGameCode;
    private String vendorPlayerUsername;
    private String vendorCurrency;
    private BigDecimal betAmount;
    /**
     * GA generated game session token during game launch.
     * Need to map if vendor returns back GA's token.
     */
    private String token;
    private String vendorSessionToken; // Vendor's game session token provided in vendor's request.
    private Long timestamp; // Vendor bet time

    // --- internal values ---
    private String traceId;
    private String vendorClassName;
    private Integer vendorId;
    private Long vendorPlayerId;
    private Integer agentId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private String currencyCode; // GA internal currency code, auto-populated for Operator API
    private Integer currencyId;
    private Integer vendorLineId;
}
