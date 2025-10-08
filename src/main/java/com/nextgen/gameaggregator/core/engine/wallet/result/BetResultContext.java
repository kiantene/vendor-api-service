package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.context.VendorGameAware;
import com.nextgen.gameaggregator.core.context.VendorPlayerAware;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class BetResultContext extends VendorRequestContext implements VendorPlayerAware, VendorGameAware {
    // --- Vendor mapping fields ---
    private String vendorBetId;
    private String roundId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winloss;
    private BigDecimal effectiveTurnover;
    private BigDecimal jackpotAmount;
    private Integer isFreeSpin;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Boolean roundEnded;

    // --- internal values ---

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
    private String gameCode;
    private Integer gameCategoryId;
    private Integer vendorLineId;
    private BigDecimal fromVendorRate;
    private Long resultTime;

    private List<BetTransaction> betTransactions;

    public boolean isRoundEnded() {
        return Optional.ofNullable(roundEnded).orElse(false);
    }
}
