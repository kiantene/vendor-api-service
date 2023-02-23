package com.nextgen.gameaggregator.vendor.cq9.api.rollin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollInDto implements WinData {
    @NotBlank
    @Size(min = 1, max = 36)
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
    private BigDecimal validbet;

    @NotNull
    @PositiveOrZero
    private BigDecimal bet;

    @NotNull
    private BigDecimal win;

    @PositiveOrZero
    private BigDecimal roomfee;

    @NotNull
    @PositiveOrZero
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
    public WinType getWinType() {
        return (this.amount.compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;
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
