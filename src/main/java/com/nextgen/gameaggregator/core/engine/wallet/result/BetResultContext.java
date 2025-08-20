package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.context.VendorGameAware;
import com.nextgen.gameaggregator.core.context.VendorPlayerAware;
import com.nextgen.gameaggregator.core.engine.game.GameSessionData;
import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Field mapping rules for Bet Result Request:
 * <p></p>
 * <p>idempotencyKey - Unique key to stop duplicate transactions.</p>
 * <p>vendorBetId    - Vendor’s bet ID that links this result to the bet.
 *                  Must match bet request vendorBetId if provided.
 *                  Default to same value as `idempotencyKey` if not mapped</p>
 * <p>roundId        - Vendor’s round ID, used for rollback when rollbackType = BY_ROUND.
 *                  Must match bet request roundId if provided.</p>
 */
@Data
@Builder
public class BetResultContext implements GameSessionData, VendorPlayerAware, VendorGameAware {
    // --- Vendor mapping fields ---
    private String idempotencyKey;
    private String vendorBetId;
    private String roundId;
    private String gameCode;
    private String vendorPlayerUsername;
    private String vendorCurrency;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winloss;
    private BigDecimal effectiveTurnover;
    private BigDecimal jackpotAmount;
    private Integer isFreeSpin;
    private String token;
    private String vendorSessionToken;
    private Long vendorBetTime;
    private Long vendorSettleTime;

    // --- internal values ---

    /**
     * A unique identifier for tracing requests across distributed services.
     * Used for debugging and logging to follow the lifecycle of a bet transaction.
     */
    private String traceId;
    private String vendorClassName;
    private Integer vendorId;
    private Long vendorPlayerId;
    private Integer agentId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private Integer currencyId;
    private String currencyCode;
    private Integer productId;
    private String productCode;
    private Integer productGameId;
    private Integer vendorGameId;
    private String gameName;
    private Integer gameCategoryId;
    private Integer vendorLineId;
    private BigDecimal fromVendorRate;
    private Long resultTime;

    private List<BetTransaction> betTransactions;
}
