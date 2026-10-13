package com.nextgen.gameaggregator.vendor.megafair.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.vendor.megafair.request.CommonRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VendorUtilTest {

    private VendorUtil vendorUtil;
    private Validator validator;
    private WalletBalanceService walletBalanceService;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        ObjectMapper objectMapper = new ObjectMapper();
        vendorUtil = new VendorUtil(validator, objectMapper, walletBalanceService);
    }

    @Test
    @DisplayName("parseFormattedFields: Happy path for bet field")
    void parseFormattedFields_HappyPath_BetOnly() {
        CommonRequest request = CommonRequest.builder()
                .bet("100|bet_tx_001")
                .win(null)
                .build();

        vendorUtil.parseFormattedFields(request);

        assertThat(request.getBetAmount()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(request.getBetTransactionId()).isEqualTo("bet_tx_001");
        assertThat(request.getWinAmount()).isNull();
        assertThat(request.getWinTransactionId()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1000.1234|tx_001", "250.50|tx_002", "100.01|tx_003"})
    @DisplayName("parseFormattedFields: Throws InvalidRequestException when amount contains decimal fraction")
    void parseFormattedFields_DecimalAmount_ThrowsException(String rawValue) {
        CommonRequest request = CommonRequest.builder()
                .bet(rawValue)
                .build();

        assertThatThrownBy(() -> vendorUtil.parseFormattedFields(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Amount must be an integer in cents for field: bet");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1000.00|tx_001", "250.0|tx_002"})
    @DisplayName("parseFormattedFields: Accepts trailing zero decimals representing whole integer amounts")
    void parseFormattedFields_TrailingZerosInteger(String rawValue) {
        CommonRequest request = CommonRequest.builder()
                .bet(rawValue)
                .build();

        vendorUtil.parseFormattedFields(request);

        String expectedAmountStr = rawValue.split("\\|")[0];
        BigDecimal expectedAmount = new BigDecimal(expectedAmountStr);

        assertThat(request.getBetAmount()).isEqualByComparingTo(expectedAmount);
        assertThat(request.getBetAmount().stripTrailingZeros().scale()).isLessThanOrEqualTo(0);
        assertThat(request.getBetTransactionId()).isEqualTo(rawValue.split("\\|")[1]);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("parseFormattedFields: Throws InvalidRequestException when present parameter is empty or blank")
    void parseFormattedFields_BlankValue_ThrowsException(String blankValue) {
        CommonRequest request = CommonRequest.builder()
                .bet(blankValue)
                .win(null)
                .build();

        assertThatThrownBy(() -> vendorUtil.parseFormattedFields(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Field 'bet' must not be blank");
    }

    @Test
    @DisplayName("parseFormattedFields: Throws InvalidRequestException on '|txId' format (missing amount)")
    void parseFormattedFields_LeadingPipeWithTxId() {
        CommonRequest request = CommonRequest.builder()
                .bet("|tx_only_123")
                .build();

        assertThatThrownBy(() -> vendorUtil.parseFormattedFields(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Missing amount for field: bet");
    }

    @Test
    @DisplayName("parseFormattedFields: Throws InvalidRequestException on 'amount|' (missing txId)")
    void parseFormattedFields_AmountWithTrailingPipe() {
        CommonRequest request = CommonRequest.builder()
                .bet("100|")
                .build();

        assertThatThrownBy(() -> vendorUtil.parseFormattedFields(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Missing transaction ID");
    }

    @Test
    @DisplayName("parseFormattedFields: Throws InvalidRequestException on '|' only (missing amount)")
    void parseFormattedFields_PipeOnly() {
        CommonRequest request = CommonRequest.builder()
                .bet("|")
                .build();

        assertThatThrownBy(() -> vendorUtil.parseFormattedFields(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Missing amount for field: bet");
    }

    @Test
    @DisplayName("parseFormattedFields: Throws InvalidRequestException when delimiter is missing")
    void parseFormattedFields_MissingDelimiter() {
        CommonRequest request = CommonRequest.builder()
                .bet("100_no_pipe_tx123")
                .build();

        assertThatThrownBy(() -> vendorUtil.parseFormattedFields(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Malformed field structure missing delimiter '|'");
    }

    @Test
    @DisplayName("parseFormattedFields: Throws InvalidRequestException when amount is non-numeric")
    void parseFormattedFields_InvalidAmountFormat() {
        CommonRequest request = CommonRequest.builder()
                .bet("invalid_amount|tx_123")
                .build();

        assertThatThrownBy(() -> vendorUtil.parseFormattedFields(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid data format for field: bet");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-100|tx_001", "-1|tx_002", "-0.01|tx_003"})
    @DisplayName("parseFormattedFields: Throws InvalidRequestException when amount is negative")
    void parseFormattedFields_NegativeAmount_ThrowsException(String rawValue) {
        CommonRequest request = CommonRequest.builder()
                .bet(rawValue)
                .build();

        assertThatThrownBy(() -> vendorUtil.parseFormattedFields(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Amount must be positive or zero for field: bet");
    }

    @Test
    @DisplayName("parseFormattedFields: Zero amount passes validation")
    void parseFormattedFields_ZeroAmount_PassesValidation() {
        CommonRequest request = CommonRequest.builder()
                .bet("0|tx_001")
                .build();

        vendorUtil.parseFormattedFields(request);

        assertThat(request.getBetAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(request.getBetTransactionId()).isEqualTo("tx_001");
    }

    @Test
    @DisplayName("validate: Fails when both bet and win parameters are sent, even if one is empty")
    void validate_BothBetAndWinSent_FailsExclusiveConstraint() {
        CommonRequest request = CommonRequest.builder()
                .userId("user123")
                .bet("100|tx_001")
                .win("") // Sent parameter, even if empty
                .roundId("round123")
                .gameId("game123")
                .gameSessionId("session123")
                .hash("hash123")
                .isRoundFinished("1")
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().contains("notBothBetAndWin"));
    }

    @Test
    @DisplayName("validate: Fails when neither bet nor win parameters are sent")
    void validate_NeitherBetNorWinSent_FailsExclusiveConstraint() {
        CommonRequest request = CommonRequest.builder()
                .userId("user123")
                .bet(null)
                .win(null)
                .roundId("round123")
                .gameId("game123")
                .gameSessionId("session123")
                .hash("hash123")
                .isRoundFinished("1")
                .build();

        Set<ConstraintViolation<CommonRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().contains("betOrWinPresent"));
    }
}
