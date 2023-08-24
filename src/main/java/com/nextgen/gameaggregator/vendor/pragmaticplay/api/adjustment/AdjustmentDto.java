package com.nextgen.gameaggregator.vendor.pragmaticplay.api.adjustment;

import com.nextgen.gameaggregator.operator.wallet.adjustment.AdjustmentData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
public class AdjustmentDto implements AdjustmentData {

    // Hash code of the request
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric allowed
    private String hash;

    // Identifier of the user within the Casino Operator’s system.
    @NotBlank
    // Size checking is done on each Action
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String userId;

    // Id of the game.
    @NotBlank @Size(min = 1, max = 32) @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    // Only alphanumeric/underscore/dash allowed
    private String gameId;

    // Token of the player from Authenticate response.
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String token;

    // Id of the round.
    @NotBlank @Size(max = 100) @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String roundId;

    // Amount of the adjustment
    @NotNull
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    // Unique reference of this transaction.
    @NotBlank
    @Size(min = 1, max = 32)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String reference;

    // Game Provider id.
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String providerId;

    // Effective Turnover of the bet. Minimum is 0.00.
    @Range(min = 0)
    @NotNull
    private BigDecimal validBetAmount;

    // Date and time when the transaction is processed on the Pragmatic Play side
    // (Unix epoch time in milliseconds, for example : 1470926696715)
    @Positive
    @NotNull
    private Long timestamp;

    @Override
    public String getVendorBetId() {
        return this.reference;
    }

    @Override
    public String getExternalTransactionId() {
        return this.reference;
    }

    @Override
    public BigDecimal getAdjustmentAmount() {
        return this.amount;
    }

    @Override
    public Long getTimestamp() {
        return this.timestamp;
    }
}
