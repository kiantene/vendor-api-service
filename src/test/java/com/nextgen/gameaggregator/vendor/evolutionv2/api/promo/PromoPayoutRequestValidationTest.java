package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evolution v2 promo-payout integration.
 */
class PromoPayoutRequestValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequest_hasNoViolations() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void invalidRequiredFields_areRejected() {
        PromoPayoutRequestDto request = validRequest();
        request.setSid("");
        request.setCurrency("US");
        request.getPromoTransaction().setId("");
        request.getPromoTransaction().setAmount(new BigDecimal("-0.01"));

        Set<ConstraintViolation<PromoPayoutRequestDto>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("sid", "currency", "promoTransaction.id", "promoTransaction.amount");
    }

    private PromoPayoutRequestDto validRequest() {
        PromoTransactionDto transaction = new PromoTransactionDto();
        transaction.setType("FreeRoundPlayableSpent");
        transaction.setId("promo-tx-123");
        transaction.setAmount(new BigDecimal("1.250000"));

        PromoPayoutRequestDto request = new PromoPayoutRequestDto();
        request.setSid("sid-123");
        request.setUserId("player123");
        request.setCurrency("USD");
        request.setUuid("request-123");
        request.setPromoTransaction(transaction);
        return request;
    }
}
