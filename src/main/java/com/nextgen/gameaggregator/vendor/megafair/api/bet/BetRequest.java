package com.nextgen.gameaggregator.vendor.megafair.api.bet;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetRequest {

    @NotBlank
    @Size(max = 255)
    private String userId;

    @NotBlank
    @Size(max = 255)
    private String bet;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 22, fraction = 0)
    private BigDecimal betAmount;

    private String betTransactionId;

    @NotBlank
    @Size(max = 255)
    private String roundId;

    @NotBlank
    @Size(max = 255)
    private String gameId;

    @NotNull
    private String isRoundFinished;

    @NotBlank
    @Size(max = 255)
    private String gameSessionId;

    private String kind;

    @NotBlank
    @Size(max = 255)
    private String hash;

    @AssertTrue(message = "roundId must not contain spaces")
    public boolean isRoundIdValid() {
        return roundId == null || !roundId.contains(" ");
    }

    @AssertTrue(message = "bet must not contain spaces")
    public boolean isBetValid() {
        return bet == null || bet.isEmpty() || !bet.contains(" ");
    }
}
