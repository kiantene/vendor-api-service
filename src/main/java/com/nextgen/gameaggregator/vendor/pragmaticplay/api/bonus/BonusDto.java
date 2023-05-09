package com.nextgen.gameaggregator.vendor.pragmaticplay.api.bonus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BonusDto implements BetResultData {

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


    // Id of the round.
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String roundId;

    // Amount of the bet. Minimum is 0.00.
    @Positive
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

    // Date and time when the transaction is processed on the Pragmatic Play side
    // (Unix epoch time in milliseconds, for example : 1470926696715)
    @Positive
    @NotNull
    private Long timestamp;

    // Token of the player from Authenticate response.
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String token;

    @Override
    public String getExternalTransactionId() {
        return this.reference;
    }

    @Override
    public String getVendorBetId() {
        return this.reference;
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        //will return as 0 is because, bonusWin end point figure will be included in the result endpoint, so we are not going to double process the winAmount again
        return BigDecimal.ZERO;
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
        return this.timestamp;
    }

    @Override
    public Long getResultTime() {
        return this.timestamp;
    }

    @Override
    public Long getVendorSettleTime() {
        return this.timestamp;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return null;
    }

    @Override
    public BetStatus getBetStatus() {
        return null;
    }
}
