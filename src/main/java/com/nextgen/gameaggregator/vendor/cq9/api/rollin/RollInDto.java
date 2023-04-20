package com.nextgen.gameaggregator.vendor.cq9.api.rollin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollInDto implements WinData {
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
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
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
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @Size(min = 1, max = 70)
    private String mtcode;

    @NotBlank
    private String createTime;

    @NotNull
    @PositiveOrZero
    private BigDecimal rake;

    @NotBlank
    private String gametype;

    @Override
    public String getExternalTransactionId() {
        return this.mtcode;
    }

    @Override
    public BigDecimal getAmount() {
        return this.amount;
    }

    @Override
    public String getRoundId() {
        return this.roundid;
    }

    @Override
    public String getGameId() {
        return this.gamecode;
    }

    @Override
    public Long getTimestamp() {
        Instant instant = Instant.parse(this.getEventTime());
        return instant.getEpochSecond();
    }

    @Override
    public ResultType getWinType() {
        return (this.amount.compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.LOSE;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        BigDecimal effectiveTurnover = BigDecimal.ZERO;
        switch(this.gametype) {
            case "fish":
            case "arcade":
                effectiveTurnover = this.bet;
                break;
            case "table":
            case "live":
                effectiveTurnover = this.validbet;
                break;
            default:
        }

        return effectiveTurnover;
    }

    @Override
    public BetResultLog prepareData(BetHistory betHistory, BetResultLog betResultLog){
        return betResultLog;
    }
}
