package com.nextgen.gameaggregator.vendor.hacksawgaming.api.endwager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.annotation.Nullable;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EndWagerDto implements BetResultData {

    @NotBlank
    @Size(max =7)
    public String brandId;

    @NotBlank
    @Size(min = 32)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String sign;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 3, max = 20)
    public String brandUid;

    @NotBlank
    @Size(min = 3, max =4)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

    @NotBlank
    @PositiveOrZero
    @Digits(integer = 16, fraction = 2)
    public BigDecimal amount;

    @NotBlank
    @Size(max = 64)
    public String roundId;

    @NotBlank
    @Size(max = 64)
    public String wagerId;

    @NotBlank
    @Size(max = 20)
    public String provider;

    @Nullable
    public String gameResult;

    @NotNull
    // 0= Unfinished, 1= Round Finish
    public Boolean isEndround;

    @Override
    public String getExternalTransactionId() {
        return this.roundId;
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
    public Long getVendorBetTime() {
        Instant instant = Instant.now();
        return instant.getEpochSecond();
    }

    @Override
    public Long getResultTime() {
        Instant instant = Instant.now();
        return instant.getEpochSecond();
    }

    @Override
    public Long getVendorSettleTime() {
        Instant instant = Instant.now();
        return instant.getEpochSecond();
    }

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
