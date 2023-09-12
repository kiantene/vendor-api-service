package com.nextgen.gameaggregator.vendor.dotconnections.api.freespin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.dto.CommonDto;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FreeSpinResultDto extends CommonDto implements BetResultData {

    @NotNull
    @PositiveOrZero
    @Digits(integer = 16, fraction = 2)
    public BigDecimal amount;

    @NotNull
    @Digits(integer = Integer.MAX_VALUE, fraction = 0)
    public Integer gameId;

    @NotBlank
    @Size(max = 50)
    public String gameName;

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
        return this.gameId.toString();
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
        return 1;
    }

    @Override
    public BetStatus getBetStatus() {
        // Default end wager as unsettled
        BetStatus betStatus = BetStatus.UNSETTLED;

        // If round ended then set to settle
        if (this.isEndround.equals("true")) {
            betStatus = BetStatus.SETTLED;
        }

        return betStatus;

    }
}