package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.common.ContextValidator;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import org.springframework.stereotype.Component;

@Component
public class PromoPayoutValidator implements ContextValidator<PromoPayoutContext> {
    private final DuplicateRequestGuard duplicateRequestGuard;

    public PromoPayoutValidator(DuplicateRequestGuard duplicateRequestGuard) {
        this.duplicateRequestGuard = duplicateRequestGuard;
    }

    public void validateOrThrow(PromoPayoutContext context) {
    }
}
