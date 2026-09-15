package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.enums.PromoType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromoPayoutValidatorTest {

    private final PromoPayoutValidator validator =
            new PromoPayoutValidator(Mockito.mock(DuplicateRequestGuard.class));

    @Test
    void throwsWhenPromoTypeIsMissing() {
        PromoPayoutContext context = PromoPayoutContext.builder().build();

        assertThatThrownBy(() -> validator.validateOrThrow(context))
                .isInstanceOf(InternalConfigurationException.class)
                .hasMessageContaining("promoType is required");
    }

    @ParameterizedTest
    @EnumSource(PromoType.class)
    void passesForEveryPromoType(PromoType promoType) {
        PromoPayoutContext context = PromoPayoutContext.builder()
                .promoType(promoType)
                .build();

        assertThatCode(() -> validator.validateOrThrow(context)).doesNotThrowAnyException();
    }
}
