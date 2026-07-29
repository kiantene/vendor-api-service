package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import com.nextgen.gameaggregator.core.context.VendorAwareContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Field mapping rules for Bet Result Request:
 * <p></p>
 * <p>idempotencyKey - Unique key to stop duplicate transactions.</p>
 * <p>vendorBetId    - Vendor’s bet ID that links this Adjustment to the bet.
 *                  Essential for Adjustment Process.
 *                  Must match bet request vendorBetId if provided.
 *                  Default to same value as `idempotencyKey` if not mapped</p>
 * <p>roundId        - Vendor’s round ID, used for rollback when rollbackType = BY_ROUND.
 *                  Must match bet request roundId if provided.</p>
 */
@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class AdjustmentContext extends VendorAwareContext {
    // --- Vendor mapping fields ---
    private String vendorBetId;
    private String roundId;
    private BigDecimal adjustmentAmount;
    private BigDecimal winAmount;

    private Long timestamp;
}
