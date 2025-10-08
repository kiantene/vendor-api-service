package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.context.VendorGameAware;
import com.nextgen.gameaggregator.core.context.VendorPlayerAware;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Field mapping rules for Bet Request:
 * <p></p>
 * <p>idempotencyKey - Unique key to stop duplicate transactions.</p>
 * <p>vendorBetId    - Vendor’s bet ID (if provided in API doc). Default to same value as `idempotencyKey` if not mapped</p>
 * <p>roundId        - Vendor’s round ID, used for rollback when rollbackType = BY_ROUND.</p>
 */
@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class BetContext extends VendorRequestContext implements VendorPlayerAware, VendorGameAware {

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
    private BigDecimal betAmount;

    private Long timestamp; // Vendor bet time

    // --- internal values ---
    private String currencyCode; // GA internal currency code, auto-populated for Operator API

    /**
     * populated by {@link com.nextgen.gameaggregator.core.context.BaseEnricher} via VendorPlayerAware
     */
    private Integer vendorId;
    private Integer agentId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private Long vendorPlayerId;
    private Integer currencyId;
    private Integer vendorLineId;

    /**
     * populated by {@link com.nextgen.gameaggregator.core.context.BaseEnricher} via VendorGameAware
     */
    private Integer vendorGameId;
    private String gameCode;
    private String gameName;
    private Integer gameCategoryId;
}
