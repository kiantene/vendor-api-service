package com.nextgen.gameaggregator.vendor.vplus.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetResultRequest {

    @NotNull
    private Long userId;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Username must contain only ASCII letters, digits, or underscores")
    private String username;

    @NotBlank
    @Size(min = 32, max = 32)
    private String transactionId;

    @NotNull
    private Long gameId;

    @NotBlank
    @Size(max = 255)
    private String gameRoundId;

    @NotNull
    private Integer completed;

    @NotNull
    private Integer type;

    @NotNull
    private Long timestamp;

    @NotNull
    @Digits(integer = 20, fraction = 6)
    @PositiveOrZero
    private BigDecimal balance;

    @NotNull
    @Digits(integer = 20, fraction = 6)
    @PositiveOrZero
    private BigDecimal totalPayout;

    @NotNull
    private Integer freeSpinning;

    @NotBlank
    @Size(max = 255)
    private String token;

    @AssertTrue(message = "Completed must be either 0 (not completed) or 1 (completed)")
    public boolean isCompletedValid() {
        return completed != null && (completed == 0 || completed == 1);
    }

    @AssertTrue(message = "Type must be one of: 0, 1, 2, 3, 4, 5, 6, 7, 101")
    public boolean isTypeValid() {
        return type != null && Set.of(0, 1, 2, 3, 4, 5, 6, 7, 101).contains(type);
    }

    @AssertTrue(message = "transactionId and gameRoundId must not contain spaces")
    public boolean isTransactionIdAndGameRoundIdValid() {
        return transactionId != null && gameRoundId != null
                && !transactionId.contains(" ")
                && !gameRoundId.contains(" ");
    }

    @AssertTrue(message = "freeSpinning must be 0 or 1")
    public boolean isFreeSpinningValid() {
        return freeSpinning != null && (freeSpinning == 0 || freeSpinning == 1);
    }
}
