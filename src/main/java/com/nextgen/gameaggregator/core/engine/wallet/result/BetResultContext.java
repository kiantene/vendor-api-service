package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.context.VendorAwareContext;
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
public class BetResultContext extends VendorAwareContext {
    // --- Vendor mapping fields ---
    private String vendorBetId;
    private String roundId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winloss;
    private BigDecimal effectiveTurnover;
    private BigDecimal jackpotAmount;
    private Integer isFreeSpin;
    private Long vendorBetTime; // Usually Not Set
    private Long vendorSettleTime;
    private Boolean roundEnded;

    private Long resultTime;

    private List<BetTransaction> betTransactions;

    public boolean isRoundEnded() {
        return Optional.ofNullable(roundEnded).orElse(false);
    }
}
