package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bean validation on {@code /promowin}, which runs during argument resolution — before the controller and
 * therefore before {@code @VendorExceptionHandler} can map anything. A violation here is answered by
 * {@code RequestValidationExceptionHandler}, which resolves the mapper by the request's vendor class name
 * ({@code "digitain"}), so it lands on the general {@code DigitainExceptionMapper} and comes back as
 * {@code err: 999 GeneralError} — not the promowin-specific mapper.
 */
class PromoWinRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * The ONEAPI-372 fix. {@code gid} carried {@code @NotBlank} while the Digitain spec marks it optional —
     * "sometimes not available, for example, in tournament wins" — so a tournament prize win ({@code opt}
     * 85) with no game id was rejected as a general error before it ever reached the payout.
     */
    @Test
    void acceptsATournamentWinWithNoGameId() {
        PromoWinRequest request = PromoWinRequests.of("85");
        request.setGid(null);

        assertThat(violationPaths(request)).isEmpty();
    }

    @Test
    void stillRejectsAPayloadMissingTheMandatoryFields() {
        PromoWinRequest request = PromoWinRequests.of("26");
        request.setPid(null);
        request.setTxid(null);
        request.setPwa(null);
        request.setCid(null);
        request.setPrid(null);

        assertThat(violationPaths(request)).contains("pid", "txid", "pwa", "cid", "prid");
    }

    private Set<String> violationPaths(PromoWinRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
