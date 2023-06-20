package com.nextgen.gameaggregator.vendor.dotconnections.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EndWagerDto implements BetResultData {

    @NotBlank
    @Size(max = 7)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String brandId;

    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[A-Z0-9]*$")
    public String sign;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 3, max = 20)
    public String brandUid;

    @NotBlank
    @Size(min = 3, max = 4)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 16, fraction = 2)
    public BigDecimal amount;

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    public String roundId;

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    public String wagerId;

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^[a-z]+$")
    public String provider;

    @Nullable
    public String gameResult;

    @NotNull
    @Pattern(regexp = "^true$|^false$")
    // 0= Unfinished, 1= Round Finish
    public String isEndround;

    @Override
    public String getExternalTransactionId() {
        return this.wagerId;
    }

    @Override
    public String getVendorBetId() {
        return this.wagerId;
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.amount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.amount;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.ZERO;
    }

    @Override
    public Long getVendorBetTime() { return null; }

    @Override
    public Long getResultTime() { return null; }

    @Override
    public Long getVendorSettleTime() { return null; }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
