package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.nextgen.gameaggregator.service.HttpService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Aviatrix {@code /transactions/promoWin} request contract.
 *
 * <p>Aviatrix added a mandatory {@code promo} object and this DTO did not model it, so every promoWin
 * failed deserialization before any validation ran (OAS-5107).
 */
class PromoWinDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /** The exact payload from OAS-5107. */
    private static final String TOURNAMENT_PAYLOAD = """
            {"cid":"zen01latam","sessionToken":"cd1a1179-6692-4766-a1e3-75d1b8160f21",
             "playerId":"3c0yvikozna","productId":"nft-aviatrix",
             "txId":"eyJiaWQiOiI1YmEwNjk1YS1kZjExLTRlOWItOWFjOC1iMmRiM2U0YTVkMGIiLCJvcCI6IlByb21vV2luIiwia2V5IjoiIn0=",
             "amount":333,"currency":"USD","promo":{"type":"tournament"}}""";

    private static final String BONUS_PAYLOAD = """
            {"cid":"somebrand","sessionToken":"some4session7token5","playerId":"someplayerid123",
             "productId":"nft-aviatrix","txId":"tx-123","amount":1000,"currency":"EUR",
             "promo":{"type":"bonus","bonusId":"some1bonus2id"}}""";

    @Test
    void parsesTheTournamentPayloadThatUsedToFail() throws Exception {
        PromoWinDto dto = HttpService.convertJsonToDto(TOURNAMENT_PAYLOAD, PromoWinDto.class);

        assertThat(dto.getPromo().getType()).isEqualTo("tournament");
        assertThat(dto.getPromo().getBonusId()).isNull();
        assertThat(dto.getAmount()).isEqualByComparingTo("333");
        assertThat(dto.getPayoutAmount()).isEqualByComparingTo("3.33");
    }

    @Test
    void parsesTheBonusPayloadWithBonusId() throws Exception {
        PromoWinDto dto = HttpService.convertJsonToDto(BONUS_PAYLOAD, PromoWinDto.class);

        assertThat(dto.getPromo().getType()).isEqualTo("bonus");
        assertThat(dto.getPromo().getBonusId()).isEqualTo("some1bonus2id");
    }

    @Test
    void ignoresFieldsTheVendorAddsLater() {
        String withUnknownField = TOURNAMENT_PAYLOAD.replace(
                "\"promo\":", "\"someFutureField\":\"x\",\"promo\":");

        assertThatCode(() -> HttpService.convertJsonToDto(withUnknownField, PromoWinDto.class))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAMissingPromoObject() throws Exception {
        String withoutPromo = TOURNAMENT_PAYLOAD.replace(",\"promo\":{\"type\":\"tournament\"}", "");

        PromoWinDto dto = HttpService.convertJsonToDto(withoutPromo, PromoWinDto.class);

        assertThat(violationPaths(dto)).contains("promo");
    }

    @Test
    void rejectsABlankPromoType() throws Exception {
        PromoWinDto dto = HttpService.convertJsonToDto(
                TOURNAMENT_PAYLOAD.replace("\"tournament\"", "\"\""), PromoWinDto.class);

        assertThat(violationPaths(dto)).contains("promo.type");
    }

    /**
     * GA-15068. Aviatrix quotes {@code amount} in minor units as an integer, so a decimal is a contract
     * violation and must be refused.
     *
     * <p>It was silently accepted while the field was a {@code BigInteger}: Jackson truncated
     * {@code 0.5678} to {@code 0} before validation, so {@code @Digits(fraction = 0)} saw an integer and
     * passed — and a zero-amount payout was recorded as settled with a 200 back to the vendor. As a
     * {@code BigDecimal} the value survives deserialization and the constraint can reject it.
     */
    @Test
    void rejectsAFractionalAmount() throws Exception {
        PromoWinDto dto = HttpService.convertJsonToDto(
                TOURNAMENT_PAYLOAD.replace("\"amount\":333", "\"amount\":0.5678"), PromoWinDto.class);

        assertThat(dto.getAmount()).isEqualByComparingTo("0.5678");
        assertThat(violationPaths(dto)).contains("amount");
    }

    @Test
    void stillAcceptsAWholeMinorUnitAmount() throws Exception {
        PromoWinDto dto = HttpService.convertJsonToDto(TOURNAMENT_PAYLOAD, PromoWinDto.class);

        assertThat(violationPaths(dto)).doesNotContain("amount");
        assertThat(dto.getPayoutAmount()).isEqualByComparingTo("3.33");
    }

    /**
     * The type-hierarchy assumption {@code PromoWinAction}'s handler depends on. Jackson's
     * {@code UnrecognizedPropertyException} is a sibling of {@code InvalidFormatException} under
     * {@code MismatchedInputException}, not a subclass — which is why the narrower catch missed it and a
     * deserialization failure surfaced as "Unknown error" rather than the spec's "Invalid request".
     */
    @Test
    void unrecognisedPropertyIsCaughtByTheWidenedHandler() {
        assertThat(MismatchedInputException.class).isAssignableFrom(UnrecognizedPropertyException.class);
    }

    private Set<String> violationPaths(PromoWinDto dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }
}
