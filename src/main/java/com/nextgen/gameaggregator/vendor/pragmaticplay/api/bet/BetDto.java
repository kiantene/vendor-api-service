package com.nextgen.gameaggregator.vendor.pragmaticplay.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements BetResultData {

    // Hash code of the request
    @NotBlank @Size(max = 100) @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric allowed
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

    // Id of the round.
    @NotBlank @Size(max = 100) @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String roundId;

    // Amount of the bet. Minimum is 0.00.
    @Range(min = 0)
    @NotNull
    private BigDecimal amount;

    // Unique reference of this transaction.
    @NotBlank @Size(min = 1, max = 32) @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    // Only alphanumeric allowed
    private String reference;

    // Game Provider id.
    @NotBlank @Size(max = 50) @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    // Only alphanumeric/underscore/dash allowed
    private String providerId;

    // Date and time when the transaction is processed on the Pragmatic Play side
    // (Unix epoch time in milliseconds, for example : 1470926696715)
    @Positive
    @NotNull
    private Long timestamp;

    // Additional information about the current game round.
    @NotBlank
    @Size(max = 4000)
    private String roundDetails;

    // Token of the player from Authenticate response.
    @NotBlank @Size(max = 50) @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    // Only alphanumeric/underscore/dash allowed
    private String token;

    @Override
    public String getExternalTransactionId() {
        return this.reference;
    }

    @Override
    public String getVendorBetId() {
        return this.roundId;
    }

    @Override
    public BigDecimal getBetAmount() {
        return amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return timestamp;
    }

    @Override
    public Long getResultTime() {
        return timestamp;
    }

    @Override
    public Long getVendorSettleTime() {
        return timestamp;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }


}
