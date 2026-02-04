package com.nextgen.gameaggregator.vendor.vplus.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRequest {

    @NotNull
    private Long userId;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Username must contain only ASCII letters, digits, or underscores")
    private String username;

    @NotBlank
    @Size(max = 32)
    private String transactionId;

    @NotNull
    private Long gameId;

    @NotBlank
    @Size(max = 255)
    private String gameRoundId;

    @NotNull
    private Integer completed;

    @NotNull
    private Long timestamp;

    @NotNull
    @Digits(integer = 20, fraction = 6)
    @PositiveOrZero
    private BigDecimal balance;

    @NotNull
    private Integer type;

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

    @AssertTrue(message = "Type must be 0")
    public boolean isTypeValid() {
        return type != null && type == 0;
    }

    @AssertTrue(message = "transactionId and gameRoundId must not contain spaces")
    public boolean isTransactionIdAndGameRoundIdValid() {
        return transactionId != null && gameRoundId != null
                && !transactionId.contains(" ")
                && !gameRoundId.contains(" ");
    }

    @AssertTrue(message = "totalPayout must be 0 when placing a bet")
    public boolean isTotalPayoutValid() {
        return totalPayout != null && BigDecimal.ZERO.compareTo(totalPayout) == 0;
    }

    @AssertTrue(message = "freeSpinning must always be 0")
    public boolean isFreeSpinningValid() {
        return freeSpinning != null && freeSpinning == 0;
    }
}
