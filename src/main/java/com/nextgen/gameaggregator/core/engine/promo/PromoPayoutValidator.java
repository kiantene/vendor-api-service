package com.nextgen.gameaggregator.core.engine.promo;

import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import org.springframework.stereotype.Service;

@Service
public class PromoPayoutValidator {
    private final DuplicateRequestGuard duplicateRequestGuard;


    public PromoPayoutValidator(DuplicateRequestGuard duplicateRequestGuard) {
        this.duplicateRequestGuard = duplicateRequestGuard;
    }

    public void validateOrThrow(String vendorClassName, PromoPayoutContext context) throws DuplicateRequestException {
        final String ACTION = "promopayout";
        final String idempotencyKey = context.getIdempotencyKey();

        duplicateRequestGuard.ensureNotDuplicate(vendorClassName, ACTION, idempotencyKey);
    }

}
