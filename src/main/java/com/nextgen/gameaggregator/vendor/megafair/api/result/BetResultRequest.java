package com.nextgen.gameaggregator.vendor.megafair.api.result;

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
public class BetResultRequest {

    @NotBlank
    @Size(max = 255)
    private String userId;

    @NotBlank
    @Size(max = 255)
    private String win;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 22, fraction = 0)
    private BigDecimal winAmount;

    private String winTransactionId;

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

    @AssertTrue(message = "win must not contain spaces")
    public boolean isWinValid() {
        return win == null || win.isEmpty() || !win.contains(" ");
    }
}
