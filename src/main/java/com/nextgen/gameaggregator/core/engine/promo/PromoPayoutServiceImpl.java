package com.nextgen.gameaggregator.core.engine.promo;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import org.springframework.stereotype.Service;

@Service
public class PromoPayoutServiceImpl implements PromoPayoutService {

    private final PromoPayoutValidator validator;

    public PromoPayoutServiceImpl(PromoPayoutValidator validator) {
        this.validator = validator;
    }

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {
        String className = "pgsoft";
        validator.validateOrThrow(className, context);

        return null;
    }
}
