package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Evolution v2 promo-payout integration.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EvolutionPromoPayoutContext extends PromoPayoutContext {
    private String vendorRequestUuid;
}
