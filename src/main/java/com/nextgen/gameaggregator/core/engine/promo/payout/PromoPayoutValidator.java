package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.common.ContextValidator;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import org.springframework.stereotype.Component;

@Component
public class PromoPayoutValidator implements ContextValidator<PromoPayoutContext> {
    private final DuplicateRequestGuard duplicateRequestGuard;

    public PromoPayoutValidator(DuplicateRequestGuard duplicateRequestGuard) {
        this.duplicateRequestGuard = duplicateRequestGuard;
    }

    /**
     * Invariants that must hold after enrichment and before processing.
     *
     * <p>{@code promoType} is required — it is forwarded to the operator on the payout request and written
     * to {@code promo_payout_history.promo_type}. Without this guard a mapper that omits it NPEs later in
     * {@code PromoPayoutHistoryProducer}, and in batch mode that NPE is swallowed by a fire-and-forget
     * subscribe, returning success to the vendor for a payout that was never recorded.
     */
    @Override
    public void validateOrThrow(PromoPayoutContext context) {
        if (context.getPromoType() == null) {
            throw new InternalConfigurationException(
                    "promoType is required on PromoPayoutContext; check the vendor request mapper");
        }
    }
}
