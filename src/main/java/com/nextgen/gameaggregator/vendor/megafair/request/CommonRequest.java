package com.nextgen.gameaggregator.vendor.megafair.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonRequest {

    @NotBlank
    @Size(max = 255)
    private String userId;

    @Size(max = 255)
    private String bet;

    @PositiveOrZero
    private BigDecimal betAmount;

    private String betTransactionId;

    @Size(max = 255)
    private String win;

    @PositiveOrZero
    private BigDecimal winAmount;

    private String winTransactionId;

    @NotBlank
    @Size(max = 255)
    private String roundId;

    @NotBlank
    @Size(max = 255)
    private String gameId;

    @NotBlank
    @Size(max = 255)
    private String isRoundFinished;

    @NotBlank
    @Size(max = 255)
    private String gameSessionId;

    private String kind;

    @NotBlank
    @Size(max = 255)
    private String hash;

    @AssertTrue(message = "userId must not contain spaces")
    public boolean isUserIdValid() {
        return userId == null || !userId.contains(" ");
    }

    @AssertTrue(message = "roundId must not contain spaces")
    public boolean isRoundIdValid() {
        return roundId == null || !roundId.contains(" ");
    }

    @AssertTrue(message = "bet must not contain spaces")
    public boolean isBetValid() {
        return bet == null || bet.isEmpty() || !bet.contains(" ");
    }

    @AssertTrue(message = "win must not contain spaces")
    public boolean isWinValid() {
        return win == null || win.isEmpty() || !win.contains(" ");
    }

    @AssertTrue(message = "Request must contain at least one of 'bet' or 'win'")
    public boolean isBetOrWinPresent() {
        return bet != null || win != null;
    }

    @AssertTrue(message = "Request cannot contain both 'bet' and 'win'")
    public boolean isNotBothBetAndWin() {
        boolean hasBet = bet != null;
        boolean hasWin = win != null;
        return !(hasBet && hasWin);
    }

    @AssertTrue(message = "gameId must not contain spaces")
    public boolean isGameIdValid() {
        return gameId == null || !gameId.contains(" ");
    }

    @AssertTrue(message = "gameSessionId must not contain spaces")
    public boolean isGameSessionIdValid() {
        return gameSessionId == null || !gameSessionId.contains(" ");
    }

    @AssertTrue(message = "isRoundFinished must be '0', '1', 'true', or 'false'")
    public boolean isIsRoundFinishedValid() {
        if (isRoundFinished == null || isRoundFinished.isBlank()) {
            return true; // Let @NotBlank handle null and empty validations
        }
        return "0".equals(isRoundFinished)
                || "1".equals(isRoundFinished)
                || "true".equalsIgnoreCase(isRoundFinished)
                || "false".equalsIgnoreCase(isRoundFinished);
    }
}
