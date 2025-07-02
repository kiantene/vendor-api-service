package com.nextgen.gameaggregator.vendor.cq9.api.rollin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollInDto implements BetResultData {
    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String account;

    @NotBlank
    private String eventTime;

    @NotBlank
    @Size(min = 1, max = 36)
    private String gamehall;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 1, max = 36)
    private String gamecode;

    @NotBlank
    @Size(min = 1, max = 50)
    private String roundid;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 12, fraction = 10)
    private BigDecimal validbet;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 12, fraction = 10)
    private BigDecimal bet;

    @NotNull
    @Digits(integer = 12, fraction = 10)
    private BigDecimal win;

    @PositiveOrZero
    @Digits(integer = 12, fraction = 10)
    private BigDecimal roomfee;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 12, fraction = 10)
    private BigDecimal amount;

    @NotBlank
    @Size(min = 1, max = 70)
    private String mtcode;

    @NotBlank
    private String createTime;

    @NotNull
    @PositiveOrZero
    private BigDecimal rake;

    @NotBlank
    private String gametype;

    public Long getTimestamp() {
        Instant instant = Instant.parse(this.getEventTime());
        return instant.toEpochMilli();
    }

    @Override
    public String getExternalTransactionId() {
        return this.mtcode;
    }

    @Override
    public String getVendorBetId() {
        return this.mtcode;
    }

    @Override
    public String getRoundId() {
        return this.roundid;
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
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {

        return switch (this.gametype) {
            case "table", "live" -> this.validbet;
            default -> this.bet;
        };
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return null;
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
