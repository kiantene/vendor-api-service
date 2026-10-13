package com.nextgen.gameaggregator.vendor.megafair.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private CommonRequest.CommonRequestBuilder createValidBaseBuilder() {
        return CommonRequest.builder()
                .userId("user123")
                .roundId("round123")
                .gameId("game123")
                .gameSessionId("session123")
                .isRoundFinished("0")
                .hash("hash123");
    }

    // ==========================================
    // Mutual Exclusivity (bet XOR win) Tests
    // ==========================================

    @Test
    @DisplayName("Valid when only bet is present")
    void shouldPassValidation_whenOnlyBetIsPresent() {
        CommonRequest request = createValidBaseBuilder()
                .bet("bet123")
                .win(null)
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Should have no validation errors when only bet is present");
    }

    @Test
    @DisplayName("Valid when only win is present")
    void shouldPassValidation_whenOnlyWinIsPresent() {
        CommonRequest request = createValidBaseBuilder()
                .bet(null)
                .win("win123")
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Should have no validation errors when only win is present");
    }

    @Test
    @DisplayName("Invalid when both bet and win are present")
    void shouldFailValidation_whenBothBetAndWinArePresent() {
        CommonRequest request = createValidBaseBuilder()
                .bet("bet123")
                .win("win123")
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<CommonRequest> violation = violations.iterator().next();
        assertEquals("Request cannot contain both 'bet' and 'win'", violation.getMessage());
    }

    @Test
    @DisplayName("Invalid when both bet and win are null")
    void shouldFailValidation_whenBothBetAndWinAreNull() {
        CommonRequest request = createValidBaseBuilder()
                .bet(null)
                .win(null)
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        boolean hasPresenceViolation = violations.stream()
                .anyMatch(v -> "betOrWinPresent".equals(v.getPropertyPath().toString())
                        && "Request must contain at least one of 'bet' or 'win'".equals(v.getMessage()));

        assertTrue(hasPresenceViolation, "Should trigger betOrWinPresent when both fields are null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("Fails notBothBetAndWin when both parameters are present even if empty or blank")
    void shouldFailNotBothBetAndWin_whenBothBetAndWinAreEmptyOrBlank(String blankValue) {
        CommonRequest request = createValidBaseBuilder()
                .bet(blankValue)
                .win(blankValue)
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        boolean hasExclusivityViolation = violations.stream()
                .anyMatch(v -> "notBothBetAndWin".equals(v.getPropertyPath().toString())
                        && "Request cannot contain both 'bet' and 'win'".equals(v.getMessage()));

        assertTrue(
                hasExclusivityViolation,
                "Non-null empty/blank strings count as present, triggering notBothBetAndWin for input: '" + blankValue + "'"
        );
    }

    // ==========================================
    // Field Space Validation Tests
    // ==========================================

    @ParameterizedTest
    @ValueSource(strings = {"user 123", " user123", "user123 "})
    @DisplayName("Invalid when userId contains spaces")
    void shouldFailValidation_whenUserIdContainsSpaces(String invalidUserId) {
        CommonRequest request = createValidBaseBuilder()
                .userId(invalidUserId)
                .bet("bet123")
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("userId must not contain spaces")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"bet 123", " bet", "bet "})
    @DisplayName("Invalid when bet contains spaces")
    void shouldFailValidation_whenBetContainsSpaces(String invalidBet) {
        CommonRequest request = createValidBaseBuilder()
                .bet(invalidBet)
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("bet must not contain spaces")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"win 123", " win", "win "})
    @DisplayName("Invalid when win contains spaces")
    void shouldFailValidation_whenWinContainsSpaces(String invalidWin) {
        CommonRequest request = createValidBaseBuilder()
                .win(invalidWin)
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("win must not contain spaces")));
    }

    // ==========================================
    // isRoundFinished Pattern Validation Tests
    // ==========================================

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "true", "false", "TRUE", "FALSE", "True", "False"})
    @DisplayName("Valid when isRoundFinished is '0', '1', 'true', or 'false' (case-insensitive)")
    void shouldPassValidation_whenIsRoundFinishedIsValidBooleanString(String value) {
        CommonRequest request = createValidBaseBuilder()
                .bet("bet123")
                .isRoundFinished(value)
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Should be valid for pattern-matched string: " + value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"test", "2", "-1", "yes", "no", "invalid"})
    @DisplayName("Invalid when isRoundFinished contains non-boolean format strings")
    void shouldFailValidation_whenIsRoundFinishedIsInvalidFormat(String value) {
        CommonRequest request = createValidBaseBuilder()
                .bet("bet123")
                .isRoundFinished(value)
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        assertTrue(
                violations.stream().anyMatch(v -> "isRoundFinished must be '0', '1', 'true', or 'false'".equals(v.getMessage())),
                "Should fail pattern validation for string: " + value
        );
    }

    @Test
    @DisplayName("Invalid when isRoundFinished is null or blank")
    void shouldFailValidation_whenIsRoundFinishedIsBlank() {
        CommonRequest request = createValidBaseBuilder()
                .bet("bet123")
                .isRoundFinished("   ")
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "isRoundFinished".equals(v.getPropertyPath().toString())));
    }
}
