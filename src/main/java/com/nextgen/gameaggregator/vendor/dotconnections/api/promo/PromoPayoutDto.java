package com.nextgen.gameaggregator.vendor.dotconnections.api.promo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PromoPayoutDto extends CommonDto implements BetResultData {

    @NotNull
    @PositiveOrZero
    @Digits(integer = 16, fraction = 2)
    public BigDecimal amount;

    @NotNull
    @Size(max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    public String promotionId;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    public String transId;

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^[a-z]+$")
    public String provider;

    @Override
    public String getExternalTransactionId() {
        return this.transId;
    }

    @Override
    public String getRoundId() {
        return this.promotionId;
    }

    @Override
    public String getVendorBetId() {
        return this.transId;
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
        return instant.toEpochMilli();
    }

    @Override
    public Long getResultTime() {
        Instant instant = Instant.now();
        return instant.toEpochMilli();
    }

    @Override
    public Long getVendorSettleTime() {
        Instant instant = Instant.now();
        return instant.toEpochMilli();
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
